package com.electrodiux.discordteams;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventsListener implements Listener {

    private boolean connectionMessages;
    private boolean playerDeathMessages;
    private boolean advancementMessages;

    public PlayerEventsListener() {
        connectionMessages = PluginMain.getConfiguration().getBoolean("discord.notifications.connection-messages",
                true);
        playerDeathMessages = PluginMain.getConfiguration().getBoolean("discord.notifications.player-death-messages",
                true);
        advancementMessages = PluginMain.getConfiguration().getBoolean("discord.notifications.advancement-messages",
                true);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (connectionMessages) {
            DiscordManager.sendGlobalMessage(
                    Messages.getMessage("player.connected", "%player%", event.getPlayer().getName()));
        }
    }

    @EventHandler
    private void onPlayerDisconnect(PlayerQuitEvent event) {
        if (connectionMessages) {
            DiscordManager.sendGlobalMessage(
                    Messages.getMessage("player.disconnected", "%player%", event.getPlayer().getName()));
        }
    }

    @EventHandler
    private void onPlayerDied(PlayerDeathEvent event) {
        if (playerDeathMessages) {
            DiscordManager.sendGlobalMessage(event.getDeathMessage());
        }
    }

    @EventHandler
    private void onAdvancementObtained(PlayerAdvancementDoneEvent event) {
        if (advancementMessages) {
            // DiscordManager.sendGlobalMessage(
            // Messages.getMessage("player.advancement-gained", "%player%",
            // event.getPlayer().getName())
            // .replace("%advancement%", event.getAdvancement().getKey().getKey()));
        }
    }

}
