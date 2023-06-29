package com.electrodiux.discordteams;

import net.md_5.bungee.api.ChatColor;

public class Messages {

    public static String getMessage(String key) {
        return PluginMain.getConfiguration().getString("messages.en." + key, key);
    }

    public static String getMessage(String key, String varName, String varValue) {
        return getMessage(key).replace(varName, varValue);
    }

    public static String getMessageWithColorCodes(String key) {
        return ChatColor.translateAlternateColorCodes('&', getMessage(key));
    }

    public static String getMessageWithColorCodes(String key, String varName, String varValue) {
        return getMessageWithColorCodes(key).replace(varName, varValue);
    }

}
