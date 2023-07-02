package com.electrodiux.discordteams.chat;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.scheduler.BukkitRunnable;

import com.electrodiux.discordteams.DiscordManager;
import com.electrodiux.discordteams.Messages;
import com.electrodiux.discordteams.PluginMain;
import com.electrodiux.discordteams.discord.LinkVerification;
import com.electrodiux.discordteams.discord.LinkedAccount;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordChatListener extends ListenerAdapter {

    private String messageFormat;
    private String messageEditedFormat;
    private String commandPrefix;

    public DiscordChatListener() {
        messageFormat = Messages.getMessage("chat.minecraft-format");
        messageEditedFormat = Messages.getMessage("chat.minecraft-edited-format");
        commandPrefix = PluginMain.getConfiguration().getString("discord.bot.minecraft-command-prefix", "!");
    }

    @Override
    public void onMessageUpdate(@Nonnull MessageUpdateEvent event) {
        if (event.getChannelType() == ChannelType.PRIVATE) {
            return;
        }

        User author = event.getAuthor();

        if (!checkMessage(event, author))
            return;

        Message message = event.getMessage();

        if (DiscordManager.isGlobalMessage(message)) {
            String messageToSend = messageEditedFormat
                    .replace("%username%", author.getEffectiveName())
                    .replace("%message%", message.getContentDisplay());
            Bukkit.broadcastMessage(messageToSend);
            return;
        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        User author = event.getAuthor();

        if (!checkMessage(event, author))
            return;

        Message message = event.getMessage();

        // check if message is a command
        if (message.getContentDisplay().startsWith(commandPrefix)) {
            processCommand(event);
            return;
        }

        // check if message is a private message
        if (event.getChannelType() == ChannelType.PRIVATE) {
            processPrivateMessage(event);
            return;
        }

        // check if message is a global message
        if (DiscordManager.isGlobalMessage(message)) {
            // normal message broadcast
            String messageToSend = messageFormat
                    .replace("%username%", author.getEffectiveName())
                    .replace("%message%", message.getContentDisplay());
            Bukkit.broadcastMessage(messageToSend);
            return;
        }

    }

    private void processCommand(MessageReceivedEvent event) {
        User author = event.getAuthor();
        Message message = event.getMessage();
        MessageChannelUnion channel = event.getChannel();

        LinkedAccount account = LinkedAccount.getAccount(author);
        if (account == null) {
            channel.sendMessage(Messages.getMessage("linking.no-linked-account")).queue();
            return;
        }

        // execute command synchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String command = message.getContentDisplay().substring(commandPrefix.length());

                    // TODO add error messages

                    Bukkit.dispatchCommand(account.getPlayer(), command);
                } catch (CommandException e) {
                    e.printStackTrace();
                    channel.sendMessage(Messages.getMessage("discord-command-execution.error")).queue();
                }
            }
        }.runTask(PluginMain.getInstance());
    }

    private static final String UNLINK_COMMAND = "unlink";

    private void processPrivateMessage(MessageReceivedEvent event) {
        User author = event.getAuthor();
        Message message = event.getMessage();

        String messageString = message.getContentDisplay();

        if (messageString.equalsIgnoreCase(UNLINK_COMMAND)) {
            LinkedAccount account = LinkedAccount.getAccount(author);
            if (account == null) {
                event.getChannel().sendMessage(Messages.getMessage("linking.bot.unlink-no-account")).queue();
                return;
            }

            LinkedAccount.unregisterAccount(account);
            return;
        }

        try {
            int code = Integer.parseInt(messageString);
            LinkVerification.checkCode(author.getName(), code, event.getChannel());
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage(Messages.getMessage("linking.bot.invalid-code-format")).queue();
            return;
        }
    }

    private boolean checkMessage(GenericMessageEvent event, User author) {
        if (author.isBot() || author.isSystem() || author.getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
            return false;
        }
        return true;
    }

}
