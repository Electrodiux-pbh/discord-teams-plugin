package com.electrodiux.discordteams;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.ChatColor;

public class Messages {

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

}
