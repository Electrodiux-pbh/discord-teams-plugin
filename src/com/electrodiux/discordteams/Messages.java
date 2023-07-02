package com.electrodiux.discordteams;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Messages {

    public static void sendMessage(@Nonnull Player player, @Nonnull String message) {
        player.sendMessage(message);
        Bukkit.getConsoleSender().sendMessage(message);
    }

    @Nonnull
    public static String getRawMessage(@Nonnull String key) {
        String msg = PluginMain.getConfiguration().getString("messages.en." + key);
        if (msg == null)
            return key;
        return msg;
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getRawMessage(@Nonnull String key, @Nonnull String varName, @Nullable String varValue) {
        return getMessage(key).replace(varName, String.valueOf(varValue));
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getMessage(@Nonnull String key) {
        return ChatColor.translateAlternateColorCodes('&', getRawMessage(key));
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getMessage(@Nonnull String key, @Nonnull String varName,
            @Nullable String varValue) {
        return ChatColor.translateAlternateColorCodes('&',
                getRawMessage(key).replace(varName, String.valueOf(varValue)));
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getMessage(@Nonnull String key, @Nonnull String varName1,
            @Nullable String varValue1, @Nonnull String varName2, @Nullable String varValue2) {
        return ChatColor.translateAlternateColorCodes('&',
                getRawMessage(key).replace(varName1, String.valueOf(varValue1)).replace(varName2,
                        String.valueOf(varValue2)));
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getMessage(@Nonnull String key, @Nonnull String varName1,
            @Nullable String varValue1, @Nonnull String varName2, @Nullable String varValue2,
            @Nonnull String varName3, @Nullable String varValue3) {
        return ChatColor.translateAlternateColorCodes('&',
                getRawMessage(key).replace(varName1, String.valueOf(varValue1)).replace(varName2,
                        String.valueOf(varValue2))
                        .replace(varName3, String.valueOf(varValue3)));
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getMessage(@Nonnull String key, @Nonnull String varName1,
            @Nullable String varValue1, @Nonnull String varName2, @Nullable String varValue2,
            @Nonnull String varName3, @Nullable String varValue3, @Nonnull String varName4,
            @Nullable String varValue4) {
        return ChatColor.translateAlternateColorCodes('&',
                getRawMessage(key).replace(varName1, String.valueOf(varValue1)).replace(varName2,
                        String.valueOf(varValue2))
                        .replace(varName3, String.valueOf(varValue3)).replace(varName4,
                                String.valueOf(varValue4)));
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getMessage(@Nonnull String key, @Nullable CommandSender sender) {
        return ChatColor.translateAlternateColorCodes('&',
                getRawMessage(key).replace("%player%", sender != null ? sender.getName() : "?"));
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getMessage(@Nonnull String key, @Nullable CommandSender sender,
            @Nonnull String varName, @Nullable String varValue) {
        return ChatColor.translateAlternateColorCodes('&',
                getRawMessage(key).replace("%player%", sender != null ? sender.getName() : "?")
                        .replace(varName, String.valueOf(varValue)));
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getMessage(@Nonnull String key, @Nullable CommandSender sender,
            @Nonnull String varName1, @Nullable String varValue1, @Nonnull String varName2,
            @Nullable String varValue2) {
        return ChatColor.translateAlternateColorCodes('&',
                getRawMessage(key).replace("%player%", sender != null ? sender.getName() : "?")
                        .replace(varName1, String.valueOf(varValue1)).replace(varName2,
                                String.valueOf(varValue2)));
    }

    @Nonnull
    @SuppressWarnings("null")
    public static String getMessage(@Nonnull String key, @Nullable CommandSender sender,
            @Nonnull String varName1, @Nullable String varValue1, @Nonnull String varName2,
            @Nullable String varValue2, @Nonnull String varName3, @Nullable String varValue3) {
        return ChatColor.translateAlternateColorCodes('&',
                getRawMessage(key).replace("%player%", sender != null ? sender.getName() : "?")
                        .replace(varName1, String.valueOf(varValue1)).replace(varName2,
                                String.valueOf(varValue2))
                        .replace(varName3,
                                String.valueOf(varValue3)));
    }

}
