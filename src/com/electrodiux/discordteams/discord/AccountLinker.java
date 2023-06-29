package com.electrodiux.discordteams.discord;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.electrodiux.discordteams.DiscordManager;
import com.electrodiux.discordteams.Messages;
import com.electrodiux.discordteams.PluginMain;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class AccountLinker {

    private UUID playerId;
    private String playerName;

    private long discordId;
    private String discordUsername;

    private int code;
    private long timeSpan;

    private AccountLinker(UUID playerId, String playerName, long discordId, String discordUsername, int code,
            long timeSpan) {
        this.playerId = playerId;
        this.playerName = playerName;

        this.discordId = discordId;
        this.discordUsername = discordUsername;

        this.code = code;
        this.timeSpan = timeSpan;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > timeSpan;
    }

    public boolean isCodeValid(int code) {
        return this.code == code;
    }

    public boolean matchDiscordUser(String discordUsername) {
        return this.discordUsername.equals(discordUsername);
    }

    private static Map<UUID, AccountLinker> links = new HashMap<>();

    public static AccountLinker createLink(Player player, String discordUsername, CommandSender sender) {
        int code = (int) (Math.random() * 1000000);

        int timeout = PluginMain.getConfiguration().getInt("discord.account-link.verification-timeout");
        long timeSpan = timeout == -1 ? Integer.MAX_VALUE : System.currentTimeMillis() + timeout * 1000;

        User discordUser = DiscordManager.getUser(discordUsername);
        if (discordUser == null) {
            sender.sendMessage(
                    Messages.getMessageWithColorCodes("linking.minecraft.user-not-found",
                            "%account%", discordUsername));
            return null;
        }

        if (links.containsKey(player.getUniqueId())) {
            AccountLinker previousLink = links.get(player.getUniqueId());
            sender.sendMessage(
                    Messages.getMessageWithColorCodes("linking.minecraft.link-reset",
                            "%account%", previousLink.getDiscordUsername()));
        }

        AccountLinker link = new AccountLinker(player.getUniqueId(), player.getName(), discordUser.getIdLong(),
                discordUsername, code, timeSpan);

        links.put(link.getPlayerId(), link);

        sender.sendMessage(
                Messages.getMessageWithColorCodes("linking.minecraft.verfication-code")
                        .replace("%code%", String.valueOf(code))
                        .replace("%timeout%", String.valueOf(timeout)));

        discordUser.openPrivateChannel().queue((channel) -> {
            channel.sendMessage(Messages.getMessage("linking.bot.linking-attempt")
                    .replace("%player%", link.getPlayerName())).queue();
        });

        return link;
    }

    public static void checkCode(String discordUsername, int code, MessageChannelUnion channel) {
        Iterator<AccountLinker> iterator = links.values().iterator();

        boolean userFound = false;

        while (iterator.hasNext()) {
            AccountLinker link = iterator.next();

            if (link.matchDiscordUser(discordUsername)) {
                userFound = true;
                if (link.isCodeValid(code)) {
                    if (link.isExpired()) {
                        iterator.remove();

                        channel.sendMessage(Messages.getMessage("linking.bot.code-expired")).queue();
                        return;
                    }
                    iterator.remove();

                    Account.registerAccount(link.getPlayerId(), link.getDiscordId());

                    channel.sendMessage(
                            Messages.getMessage("linking.bot.link-success", "%player%", link.getPlayerName())).queue();

                    Player player = Bukkit.getPlayer(link.getPlayerId());
                    player.sendMessage(Messages.getMessageWithColorCodes("linking.minecraft.link-success")
                            .replace("%account%", link.getDiscordUsername()));

                    return;
                }
            }
        }

        if (userFound) {
            channel.sendMessage(Messages.getMessage("linking.bot.invalid-code")).queue();
        } else {
            channel.sendMessage(Messages.getMessage("linking.bot.no-link-code")).queue();
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getDiscordUsername() {
        return discordUsername;
    }

    public int getCode() {
        return code;
    }

    public long getTimeSpan() {
        return timeSpan;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getDiscordId() {
        return discordId;
    }

    public static Map<UUID, AccountLinker> getLinks() {
        return links;
    }

    public static void setLinks(Map<UUID, AccountLinker> links) {
        AccountLinker.links = links;
    }

}
