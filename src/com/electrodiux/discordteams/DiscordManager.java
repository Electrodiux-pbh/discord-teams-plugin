package com.electrodiux.discordteams;

import java.util.List;

import javax.security.auth.login.LoginException;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.electrodiux.discordteams.chat.DiscordChatListener;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordManager {

    private static JavaPlugin plugin;
    private static JDA api;

    private static TextChannel globalChannel;

    private static Category teamsCategory;

    public static boolean setup(JavaPlugin plugin) {
        DiscordManager.plugin = plugin;

        String botToken = PluginMain.getConfiguration().getString("discord.bot.token");

        if (botToken == null || botToken.isBlank()) {
            plugin.getLogger().warning("No bot token was found in the config! Please add one and restart the server.");
            return false;
        }

        boolean settedup = setupDiscord(botToken);

        if (settedup) {
            globalChannel = getTextChannel(PluginMain.getConfiguration().getString("discord.global-channel-id"));
            teamsCategory = getCategory(PluginMain.getConfiguration().getString("discord.teams-category-id"));
        }

        return settedup;
    }

    private static boolean setupDiscord(String botToken) {
        try {
            JDABuilder builder = JDABuilder.createDefault(botToken);

            configureMemoryUsage(builder);
            configureActivity(builder);

            builder.addEventListeners(new DiscordChatListener());

            api = builder.build();
            if (api == null) {
                throw new LoginException("Couldn't login in to Discord!");
            }

            api.awaitReady();

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("An error occurred while setting up Discord!");
            e.printStackTrace();
            return false;
        }
    }

    private static void configureMemoryUsage(JDABuilder builder) {
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
    }

    private static void configureActivity(JDABuilder builder) {
        String activityType = PluginMain.getConfiguration().getString("discord.bot.activity.type");
        String activityText = PluginMain.getConfiguration().getString("discord.bot.activity.text");

        if (activityType != null || activityText != null) {
            ActivityType type = ActivityType.valueOf(activityType);
            if (type != null) {
                builder.setActivity(Activity.of(type, activityText));
            }
        }
    }

    public static void shutdown() {
        if (api == null)
            return;

        api.shutdown();
    }

    public static TextChannel getTextChannel(String id) {
        if (id != null) {
            TextChannel channel = api.getTextChannelById(id);
            if (channel != null) {
                Bukkit.getConsoleSender().sendMessage("Found channel: " + channel.getName());
                return channel;
            }
            Bukkit.getConsoleSender().sendMessage("Could not find channel with ID: " + id);
        }
        return null;
    }

    public static Category getCategory(String id) {
        if (id != null) {
            Category category = api.getCategoryById(id);
            if (category != null) {
                Bukkit.getConsoleSender().sendMessage("Found category: " + category.getName());
                return category;
            }
            Bukkit.getConsoleSender().sendMessage("Could not find category with ID: " + id);
        }
        return null;
    }

    public static User getUser(String discordUsername) {
        List<User> users = api.getUsersByName(discordUsername, false);

        Bukkit.getConsoleSender().sendMessage(String.valueOf(users.size()));
        if (users.size() <= 0) {
            return null;
        }

        User user = users.get(0);

        if (user != null) {
            Bukkit.getConsoleSender().sendMessage("Found user: " + user.getName());
            return user;
        }
        Bukkit.getConsoleSender().sendMessage("Could not find user with username: " + discordUsername);
        return null;
    }

    public static JDA getJDA() {
        return api;
    }

    public static TextChannel getGlobalChannel() {
        return globalChannel;
    }

    public static Category getTeamsCategory() {
        return teamsCategory;
    }

    public static void sendGlobalMessage(String message) {
        if (globalChannel != null) {
            globalChannel.sendMessage(message).queue();
        }
    }

}
