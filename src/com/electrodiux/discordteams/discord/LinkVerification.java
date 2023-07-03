package com.electrodiux.discordteams.discord;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.electrodiux.discordteams.DiscordManager;
import com.electrodiux.discordteams.DiscordTeams;
import com.electrodiux.discordteams.Messages;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class LinkVerification {

    private static final int CODE_LENGTH = 6;

    private UUID playerId;
    private String playerName;

    private long discordId;
    private String discordUsername;

    private int code;
    private long timeSpan;

    private LinkVerification(UUID playerId, String playerName, long discordId, String discordUsername, int code,
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

    private static Map<UUID, LinkVerification> links = new HashMap<>();

    public static int generateCode() {
        Random random = new Random();
        int min = (int) Math.pow(10, CODE_LENGTH - 1);
        int max = (int) Math.pow(10, CODE_LENGTH) - 1;
        return random.nextInt(max - min + 1) + min;
    }

    public static LinkVerification createLink(@Nonnull Player player, @Nonnull String discordUsername,
            @Nonnull CommandSender sender) {
        int code = generateCode();

        int timeout = DiscordTeams.getConfiguration().getInt("discord.account-link.verification-timeout");
        long timeSpan = timeout == -1 ? Integer.MAX_VALUE : System.currentTimeMillis() + timeout * 1000;

        User discordUser = DiscordManager.getUser(discordUsername);
        if (discordUser == null) {
            sender.sendMessage(
                    Messages.getMessage("linking.minecraft.user-not-found",
                            "%account%", discordUsername));
            return null;
        }

        if (links.containsKey(player.getUniqueId())) {
            LinkVerification previousLink = links.get(player.getUniqueId());
            sender.sendMessage(
                    Messages.getMessage("linking.minecraft.link-reset",
                            "%account%", previousLink.getDiscordUsername()));
        }

        LinkVerification link = new LinkVerification(player.getUniqueId(), player.getName(), discordUser.getIdLong(),
                discordUsername, code, timeSpan);

        links.put(link.getPlayerId(), link);

        sender.sendMessage(
                Messages.getMessage("linking.minecraft.verfication-code")
                        .replace("%code%", String.valueOf(code))
                        .replace("%timeout%", String.valueOf(timeout)));

        discordUser.openPrivateChannel().queue((channel) -> {
            channel.sendMessage(Messages.getMessage("linking.bot.linking-attempt",
                    "%player%", player.getName())).queue();
        });

        return link;
    }

    public static void checkCode(String discordUsername, int code, MessageChannelUnion channel) {
        Iterator<LinkVerification> iterator = links.values().iterator();

        boolean userFound = false;

        while (iterator.hasNext()) {
            LinkVerification link = iterator.next();

            if (link.matchDiscordUser(discordUsername)) {
                userFound = true;
                if (link.isCodeValid(code)) {
                    if (link.isExpired()) {
                        iterator.remove();

                        channel.sendMessage(Messages.getMessage("linking.bot.code-expired")).queue();
                        return;
                    }
                    iterator.remove();

                    LinkedAccount.registerAccount(link.getPlayerId(), link.getDiscordId());

                    channel.sendMessage(
                            Messages.getMessage("linking.bot.link-success", "%player%", link.getPlayerName())).queue();

                    Player player = Bukkit.getPlayer(link.getPlayerId());
                    player.sendMessage(Messages.getMessage("linking.minecraft.link-success",
                            "%account%", link.getDiscordUsername()));

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

    public static Map<UUID, LinkVerification> getLinks() {
        return links;
    }

    public static void setLinks(Map<UUID, LinkVerification> links) {
        LinkVerification.links = links;
    }

}
