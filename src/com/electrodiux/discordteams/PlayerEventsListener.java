package com.electrodiux.discordteams;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.electrodiux.discordteams.team.Team;

public class PlayerEventsListener implements Listener {

    private boolean connectionMessages;
    private boolean playerDeathMessages;
    private boolean advancementMessages;

    public PlayerEventsListener() {
        connectionMessages = DiscordTeams.getConfiguration().getBoolean("discord.notifications.connection-messages",
                true);
        playerDeathMessages = DiscordTeams.getConfiguration().getBoolean("discord.notifications.player-death-messages",
                true);
        advancementMessages = DiscordTeams.getConfiguration().getBoolean("discord.notifications.advancement-messages",
                true);
    }

    @EventHandler
    @SuppressWarnings("null")
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (connectionMessages) {
            DiscordManager.sendGlobalMessage(
                    Messages.getMessage("player.connected", "%player%", event.getPlayer().getName()));
        }

        Team.updatePlayerDisplayTag(event.getPlayer());
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
            String deathMessage = event.getDeathMessage();
            if (deathMessage != null) {
                DiscordManager.sendGlobalMessage(deathMessage);
            }
        }
    }

    @EventHandler
    private void onAdvancementObtained(PlayerAdvancementDoneEvent event) {
        if (advancementMessages) {
            // DiscordManager.sendGlobalMessage(
            // Messages.getMessage("player.advancement-gained", "%player%",
            // event.getPlayer().getName())
            // .replace("%advancement%", event.getAdvancement().getKey().getKey()));

            // Need to get
        }
    }

}
