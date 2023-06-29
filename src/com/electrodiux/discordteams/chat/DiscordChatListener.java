package com.electrodiux.discordteams.chat;

import org.bukkit.Bukkit;

import com.electrodiux.discordteams.Messages;
import com.electrodiux.discordteams.discord.AccountLinker;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordChatListener extends ListenerAdapter {

    private String messageFormat;

    public DiscordChatListener() {
        messageFormat = Messages.getMessageWithColorCodes("chat.minecraft-format");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannelType() == ChannelType.PRIVATE) {
            processPrivateMessage(event);
            return;
        }

        User author = event.getAuthor();
        Message message = event.getMessage();

        if (author.isBot() || author.isSystem() || author.getIdLong() == event.getJDA().getSelfUser().getIdLong()
                || !event.isFromGuild()) {
            return;
        }

        String messageToSend = messageFormat
                .replace("%username%", author.getEffectiveName())
                .replace("%message%", message.getContentDisplay());
        Bukkit.broadcastMessage(messageToSend);
    }

    private void processPrivateMessage(MessageReceivedEvent event) {
        User author = event.getAuthor();
        Message message = event.getMessage();

        if (author.isBot() || author.isSystem()) {
            return;
        }

        try {
            int code = Integer.parseInt(message.getContentDisplay());
            AccountLinker.checkCode(author.getName(), code, event.getChannel());
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage(Messages.getMessage("linking.bot.invalid-code-format")).queue();
            return;
        }
    }

}
