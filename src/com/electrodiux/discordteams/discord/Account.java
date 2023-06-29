package com.electrodiux.discordteams.discord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.Yaml;

import com.electrodiux.discordteams.DiscordManager;
import com.google.common.collect.Lists;

import net.dv8tion.jda.api.entities.User;

public class Account implements Serializable {

    private long discordUserId;
    private transient User discordUser;

    private UUID minecraftPlayerId;
    private transient Player minecraftPlayer;

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

    public Player getMinecraftPlayer() {
        if (minecraftPlayer == null) {
            minecraftPlayer = Bukkit.getPlayer(minecraftPlayerId);
        }
        return minecraftPlayer;
    }

    private static List<Account> accounts = new ArrayList<>();
    private static File dataFile;

    public static void registerAccount(UUID playerId, long userId) {
        accounts.add(new Account(playerId, userId));
    }

    public static Account getAccountByMinecraftId(UUID minecraftPlayerId) {
        for (Account account : accounts) {
            if (account.minecraftPlayerId.equals(minecraftPlayerId)) {
                return account;
            }
        }
        return null;
    }

    public static List<Account> getAccounts() {
        return accounts;
    }

    public static void saveAccounts() {
        if (accounts == null)
            return;

        Yaml yaml = new Yaml();

        if (dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                System.err.println("Failed to save accounts to " + dataFile.getAbsolutePath());
                e.printStackTrace();
            }
        }

        try {
            yaml.dump(accounts, new FileWriter(dataFile));
        } catch (IOException e) {
            System.err.println("Failed to save accounts to " + dataFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadAccounts(File file) {
        Objects.requireNonNull(file, "File cannot be null");

        dataFile = file;

        if (dataFile.exists()) {
            Yaml yaml = new Yaml();

            try {
                accounts = yaml.loadAs(new FileInputStream(dataFile), List.class);
            } catch (IOException | ClassCastException e) {
                System.err.println("Failed to load accounts from " + dataFile.getAbsolutePath());
                e.printStackTrace();
            }

            return;
        }

        dataFile.getParentFile().mkdirs();

        accounts = Lists.newArrayList();
    }

}
