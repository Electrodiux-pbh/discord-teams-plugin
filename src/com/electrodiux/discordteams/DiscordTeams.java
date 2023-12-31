package com.electrodiux.discordteams;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.electrodiux.discordteams.chat.MinecraftChatListener;
import com.electrodiux.discordteams.discord.LinkedAccount;
import com.electrodiux.discordteams.team.DiscordTeam;

public class DiscordTeams extends JavaPlugin {

    private boolean enabled = false;

    private static ConfigManager configManager;
    private static DiscordTeams instance;

    private PluginDescriptionFile descriptionFile = getDescription();

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.setupConfig();

        if (!DiscordManager.setup(this)) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        LinkedAccount.loadAccounts(new File(getDataFolder(), "data/accounts.yml"));
        DiscordTeam.loadTeams(new File(getDataFolder(), "teams"));

        // Register events
        Bukkit.getPluginManager().registerEvents(new PlayerEventsListener(), this);

        getServer().getPluginManager().registerEvents(new MinecraftChatListener(), this);

        // Register commands
        TeamCommand command = new TeamCommand();
        Bukkit.getPluginCommand("team").setExecutor(command);
        // Bukkit.getPluginCommand("team").setTabCompleter(command);

        // Plugin enabled

        notifyStart();
        Bukkit.getConsoleSender().sendMessage(
                Messages.getMessage("plugin-enabled")
                        .replace("%name%", descriptionFile.getName())
                        .replace("%version%", descriptionFile.getVersion()));
        enabled = true;
    }

    @Override
    public void onDisable() {
        if (!enabled)
            return;
        notifyStop();

        DiscordManager.shutdown();

        LinkedAccount.saveAccounts();
        DiscordTeam.saveTeams();

        Bukkit.getConsoleSender()
                .sendMessage(descriptionFile.getName() + " v" + descriptionFile.getVersion() + " has been disabled!");
        enabled = false;
    }

    private void notifyStart() {
        boolean nofifyStart = DiscordTeams.getConfiguration().getBoolean("discord.notifications.server-start", true);
        if (nofifyStart) {
            DiscordManager.sendGlobalMessage(Messages.getMessage("server.start"));
        }
    }

    private void notifyStop() {
        boolean nofifyStop = DiscordTeams.getConfiguration().getBoolean("discord.notifications.server-stop", true);
        if (nofifyStop) {
            DiscordManager.sendGlobalMessage(Messages.getMessage("server.stop"));
        }
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static FileConfiguration getConfiguration() {
        return configManager.getConfig();
    }

    public static DiscordTeams getInstance() {
        return instance;
    }

    public static void log(String message) {
        getInstance().getLogger().info(message);
    }

}