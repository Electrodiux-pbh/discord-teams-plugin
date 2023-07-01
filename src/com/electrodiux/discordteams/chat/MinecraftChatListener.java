package com.electrodiux.discordteams.chat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.electrodiux.discordteams.DiscordManager;
import com.electrodiux.discordteams.Messages;

public class MinecraftChatListener implements Listener {

    private String messageFormat;

    public MinecraftChatListener() {
        messageFormat = Messages.getMessage("chat.discord-format");
    }

    @EventHandler
    @SuppressWarnings("null")
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String messageToSend = messageFormat.replace("%username%", event.getPlayer().getName())
                .replace("%message%", event.getMessage());
        DiscordManager.sendGlobalMessage(messageToSend);
    }

}
