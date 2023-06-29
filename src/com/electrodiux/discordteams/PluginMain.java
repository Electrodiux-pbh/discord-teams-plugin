package com.electrodiux.discordteams;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import com.electrodiux.discordteams.chat.MinecraftChatListener;
import com.electrodiux.discordteams.discord.Account;

public class PluginMain extends JavaPlugin {

    private boolean enabled = false;

    private static ConfigManager configManager;

    private PluginDescriptionFile descriptionFile = getDescription();

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.setupConfig();

        if (!DiscordManager.setup(this)) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Account.loadAccounts(new File(getDataFolder(), "data/accounts.yml"));

        // Register events
        Bukkit.getPluginManager().registerEvents(new PlayerEventsListener(), this);

        getServer().getPluginManager().registerEvents(new MinecraftChatListener(), this);

        // Register commands
        TeamCommand command = new TeamCommand();
        Bukkit.getPluginCommand("team").setExecutor(command);
        Bukkit.getPluginCommand("team").setTabCompleter(command);

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

        DiscordManager.shutdown();
        Account.saveAccounts();

        notifyStop();
        Bukkit.getConsoleSender()
                .sendMessage(descriptionFile.getName() + " v" + descriptionFile.getVersion() + " has been disabled!");
        enabled = false;
    }

    @Override
    public void onLoad() {

    }

    private void notifyStart() {
        boolean nofifyStart = PluginMain.getConfiguration().getBoolean("discord.notifications.server-start", true);
        if (nofifyStart) {
            DiscordManager.sendGlobalMessage(Messages.getMessage("server.start"));
        }
    }

    private void notifyStop() {
        boolean nofifyStop = PluginMain.getConfiguration().getBoolean("discord.notifications.server-stop", true);
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

}