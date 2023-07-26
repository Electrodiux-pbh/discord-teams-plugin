package com.electrodiux.discordteams;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import com.electrodiux.discordteams.chat.DiscordChatListener;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordManager {

    private static JavaPlugin plugin;
    private static JDA api;

    private static Guild guild;
    private static TextChannel globalChannel;
    private static Category teamsCategory;

    public static boolean setup(JavaPlugin plugin) {
        DiscordManager.plugin = plugin;

        String botToken = DiscordTeams.getConfiguration().getString("discord.bot.token");

        if (botToken == null || botToken.isBlank()) {
            plugin.getLogger().warning("No bot token was found in the config! Please add one and restart the server.");
            return false;
        }

        boolean settedup = setupDiscord(botToken);

        if (settedup) {
            guild = getGuildById(DiscordTeams.getConfiguration().getLong("discord.server-id", 0));
            globalChannel = getTextChannel(DiscordTeams.getConfiguration().getLong("discord.global-channel-id", 0));
            teamsCategory = getCategory(DiscordTeams.getConfiguration().getLong("discord.teams-category-id", 0));

            if (globalChannel == null || teamsCategory == null || guild == null) {
                throw new IllegalStateException(
                        "An error occurred while searching, please check your config and try again.");
            }
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

        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.enableIntents(GatewayIntent.DIRECT_MESSAGES);
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
    }

    private static void configureActivity(JDABuilder builder) {
        String activityType = DiscordTeams.getConfiguration().getString("discord.bot.activity.type");
        String activityText = DiscordTeams.getConfiguration().getString("discord.bot.activity.text");

        if (activityType != null && activityText != null) {
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

    public static TextChannel getTextChannel(long id) {
        if (id != 0) {
            TextChannel channel = guild.getTextChannelById(id);
            if (channel != null) {
                Bukkit.getConsoleSender().sendMessage("Found text channel: " + channel.getName());
                return channel;
            }
            Bukkit.getConsoleSender().sendMessage("Could not find text channel with ID: " + id);
        }
        return null;
    }

    public static VoiceChannel getVoiceChannel(long id) {
        if (id != 0) {
            VoiceChannel channel = guild.getVoiceChannelById(id);
            if (channel != null) {
                Bukkit.getConsoleSender().sendMessage("Found voice channel: " + channel.getName());
                return channel;
            }
            Bukkit.getConsoleSender().sendMessage("Could not find voice channel with ID: " + id);
        }
        return null;
    }

    public static Category getCategory(long id) {
        if (id != 0) {
            Category category = guild.getCategoryById(id);
            if (category != null) {
                Bukkit.getConsoleSender().sendMessage("Found category: " + category.getName());
                return category;
            }
            Bukkit.getConsoleSender().sendMessage("Could not find category with ID: " + id);
        }
        return null;
    }

    public static Guild getGuildById(long id) {
        if (id != 0) {
            Guild guild = api.getGuildById(id);
            if (guild != null) {
                Bukkit.getConsoleSender().sendMessage("Found guild: " + guild.getName());
                return guild;
            }
            Bukkit.getConsoleSender().sendMessage("Could not find guild with ID: " + id);
        }
        return null;
    }

    public static Role getRole(long id) {
        if (id != 0) {
            Role role = guild.getRoleById(id);
            if (role != null) {
                Bukkit.getConsoleSender().sendMessage("Found role: " + role.getName());
                return role;
            }
            Bukkit.getConsoleSender().sendMessage("Could not find role with ID: " + id);
        }
        return null;
    }

    @Nullable
    public static User getUser(@Nonnull String discordUsername) {
        List<User> users = api.getUsersByName(discordUsername, false);

        if (users.size() <= 0) {
            // guild.getMembersByName(discordUsername, false);
            return null;
        }

        User user = users.get(0);

        if (user != null) {
            Bukkit.getConsoleSender().sendMessage("Found user: " + user.getName());
            return user;
        }
        Bukkit.getConsoleSender().sendMessage("Could not find user with username: " +
                discordUsername);
        return null;
    }

    @Nullable
    public static User getUser(long id) {
        User user = api.getUserById(id);

        if (user != null) {
            Bukkit.getConsoleSender().sendMessage("Found user: " + user.getName());
            return user;
        }

        // user = api.retrieveUserById(id).complete();

        Bukkit.getConsoleSender().sendMessage("Could not find user with ID: " + id);
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

    public static Guild getServer() {
        return guild;
    }

    public static void sendGlobalMessage(@Nonnull String message) {
        if (globalChannel != null) {
            globalChannel.sendMessage(message).queue();
        }
    }

    public static boolean isGlobalMessage(@Nonnull Message message) {
        return message.getChannel().getIdLong() == globalChannel.getIdLong();
    }

    public static Member getMember(@Nonnull User user) {
        return guild.getMember(user);
    }

    public static int getDiscordColor(ChatColor color) {
        return DiscordTeams.getConfiguration().getInt("discord.colors." + color.name().toLowerCase(), 0);
    }

}
