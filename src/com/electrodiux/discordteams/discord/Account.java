package com.electrodiux.discordteams.discord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.electrodiux.discordteams.DiscordManager;
import com.electrodiux.discordteams.Messages;
import com.electrodiux.discordteams.Team;

import net.dv8tion.jda.api.entities.User;

public class Account {

    private long discordUserId;
    private transient User discordUser;

    private UUID minecraftPlayerId;
    private transient Player minecraftPlayer;

    // for serialization
    private Account() {
    }

    private Account(UUID minecraftPlayerId, long discordUserId) {
        this.discordUserId = discordUserId;
        this.minecraftPlayerId = minecraftPlayerId;
    }

    public User getDiscordUser() {
        if (discordUser == null) {
            discordUser = DiscordManager.getJDA().getUserById(discordUserId);
        }
        return discordUser;
    }

    public Player getPlayer() {
        if (minecraftPlayer == null) {
            minecraftPlayer = Bukkit.getPlayer(minecraftPlayerId);
        }
        return minecraftPlayer;
    }

    public long getDiscordUserId() {
        return discordUserId;
    }

    public UUID getMinecraftPlayerId() {
        return minecraftPlayerId;
    }

    private void loadFromString(String data) {
        String[] parts = data.split(";");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid data string: " + data);
        }

        minecraftPlayerId = UUID.fromString(parts[0]);
        try {
            discordUserId = Long.parseLong(parts[1]);
            return;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid discord id format: " + parts[1]);
        }
    }

    @Override
    public String toString() {
        return minecraftPlayerId.toString() + ";" + discordUserId;
    }

    private static List<Account> accounts = new ArrayList<>();
    private static File dataFile;

    public static void registerAccount(UUID playerId, long userId) {
        Account account = new Account(playerId, userId);
        accounts.add(account);
        Team.syncAccount(account);

        saveAccounts();
    }

    public static void unregisterAccount(@Nonnull Account account) {
        accounts.remove(account);
        Team.syncAccount(account);

        Player player = account.getPlayer();
        User user = account.getDiscordUser();

        player.sendMessage(
                Messages.getMessage("linking.minecraft.unlink-success", "%account%", user.getName()));

        if (user != null) {
            user.openPrivateChannel().queue((channel) -> {
                channel.sendMessage(Messages.getMessage("linking.bot.unlink-success", "%player%", player.getName()))
                        .queue();
            });
        }

        saveAccounts();
    }

    @Nullable
    public static Account getAccountByMinecraftId(@NotNull UUID minecraftPlayerId) {
        for (Account account : accounts) {
            if (account.minecraftPlayerId.equals(minecraftPlayerId)) {
                return account;
            }
        }
        return null;
    }

    @Nullable
    public static Account getAccount(@NotNull Player player) {
        return getAccountByMinecraftId(player.getUniqueId());
    }

    @Nullable
    public static Account geAccountByDiscordId(long discordUserId) {
        for (Account account : accounts) {
            if (account.discordUserId == discordUserId) {
                return account;
            }
        }
        return null;
    }

    @Nullable
    public static Account getAccount(@NotNull User user) {
        return geAccountByDiscordId(user.getIdLong());
    }

    public static void saveAccounts() {
        if (accounts == null)
            return;

        if (dataFile == null) {
            System.err.println("Data file is not set. Cannot save accounts.");
            return;
        }

        if (dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                System.err.println("Failed to save accounts to " + dataFile.getAbsolutePath());
                e.printStackTrace();
                return;
            }
        }

        YamlConfiguration config = new YamlConfiguration();

        try {
            List<String> playerLinks = new ArrayList<>();
            for (Account account : accounts) {
                playerLinks.add(account.toString());
            }

            config.set("players", playerLinks);
            config.save(dataFile);

            Bukkit.getConsoleSender().sendMessage("Accounts saved: " + accounts.size());
        } catch (IOException e) {
            System.err.println("Failed to save accounts to " + dataFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    public static void loadAccounts(@Nonnull File file) {
        Objects.requireNonNull(file, "File cannot be null");

        dataFile = file;
        accounts = new ArrayList<>();

        if (dataFile.exists()) {
            YamlConfiguration config = new YamlConfiguration();

            try {
                config.load(dataFile);

                List<String> playerLinks = config.getStringList("players");
                if (playerLinks != null) {
                    for (String linkData : playerLinks) {
                        Account account = new Account();
                        account.loadFromString(linkData);
                        accounts.add(account);
                    }
                }

                Bukkit.getConsoleSender().sendMessage("Accounts loaded: " + accounts.size());
            } catch (IOException | InvalidConfigurationException e) {
                System.err.println("Failed to load accounts from " + dataFile.getAbsolutePath());
                e.printStackTrace();
            }

            return;
        }

        dataFile.getParentFile().mkdirs();
    }

}
