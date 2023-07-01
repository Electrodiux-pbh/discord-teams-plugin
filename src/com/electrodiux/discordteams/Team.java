package com.electrodiux.discordteams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.electrodiux.discordteams.discord.Account;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

public class Team {

    public static final ChatColor DEFAULT_COLOR = ChatColor.WHITE;

    private transient boolean isDeleted;

    private UUID uuid;
    private String name;
    private String tag;

    private ChatColor color;

    private List<Player> players;

    private boolean open;
    private boolean pvp;

    // discord channels
    private TextChannel textChannel;
    private VoiceChannel voiceChannel;
    private Role discordRole;

    // empty constructor for loading the configuration
    private Team() {
        this.players = new ArrayList<>();
    }

    private Team(UUID uuid, String name, String tag) {
        this();
        this.uuid = uuid;
        this.name = name;
        this.tag = tag;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;

        if (discordRole != null) {
            discordRole.getManager().setName(name).queue();
        }

        if (textChannel != null) {
            textChannel.getManager().setName(name).queue();
        }

        if (voiceChannel != null) {
            voiceChannel.getManager().setName(name).queue();
        }

        saveTeam();
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
        saveTeam();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void addPlayer(@Nonnull Player player) {
        this.players.add(player);

        syncAccount(player);

        saveTeam();
    }

    public void removePlayer(@Nonnull Player player) {
        this.players.remove(player);

        syncAccount(player);

        if (this.players.size() <= 0) {
            delete();
            return;
        }

        saveTeam();
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        saveTeam();
    }

    public boolean isPvp() {
        return pvp;
    }

    public void setPvp(boolean pvp) {
        this.pvp = pvp;
        saveTeam();
    }

    public ChatColor getColor() {
        return color;
    }

    public void setColor(ChatColor color) {
        this.color = color;

        if (discordRole != null) {
            discordRole.getManager().setColor(getColorValue(color)).queue();
        }

        saveTeam();
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public VoiceChannel getVoiceChannel() {
        return voiceChannel;
    }

    public void sendDiscordMessage(@Nonnull String message) {
        if (textChannel != null) {
            textChannel.sendMessage(message).queue();
        }
    }

    public void sendMinecraftMessage(@Nonnull String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

    @SuppressWarnings("null")
    public void sendMessage(@Nonnull String message) {
        sendDiscordMessage(ChatColor.stripColor(message));
        sendMinecraftMessage(message);
    }

    public static void syncAccount(@Nonnull Account account) {
        Player player = account.getPlayer();
        if (player == null) {
            return;
        }

        Team team = getPlayerTeam(player);
        if (team != null) {
            team.syncAccount(player, account);
        }
    }

    private void syncAccount(@Nonnull Player player) {
        Account account = Account.getAccount(player);
        if (account != null) {
            syncAccount(player, account);
        }
    }

    @SuppressWarnings("null")
    private void syncAccount(@Nonnull Player player, @Nonnull Account account) {
        User discordUser = account.getDiscordUser();
        if (discordUser == null) {
            return;
        }

        if (discordRole != null) {
            if (containsPlayer(player)) {
                DiscordManager.getServer().addRoleToMember(discordUser, discordRole).queue();
            } else {
                DiscordManager.getServer().removeRoleFromMember(discordUser, discordRole).queue();
            }
        }
    }

    public boolean containsPlayer(Player player) {
        return players.contains(player);
    }

    public void saveTeam() {
        if (isDeleted || teamsFolder == null) {
            return;
        }

        if (!teamsFolder.exists()) {
            teamsFolder.mkdirs();
        }

        YamlConfiguration config = new YamlConfiguration();
        saveTeamToConfig(config);

        try {
            File teamFile = new File(teamsFolder, uuid.toString() + ".yml");
            if (!teamFile.exists()) {
                teamFile.createNewFile();
            }
            config.save(teamFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveTeamToConfig(YamlConfiguration config) {
        config.set("uuid", uuid.toString());
        config.set("name", name);
        config.set("tag", tag);

        config.set("color-code", color != null ? color.getChar() : DEFAULT_COLOR.getChar());

        config.set("open", open);
        config.set("pvp", pvp);

        String[] playerUUIDs = new String[players.size()];
        int i = 0;
        for (Player player : players) {
            playerUUIDs[i] = player.getUniqueId().toString();
            i++;
        }

        config.set("players", playerUUIDs);

        config.set("text-channel", textChannel != null ? textChannel.getIdLong() : null);
        config.set("voice-channel", voiceChannel != null ? voiceChannel.getIdLong() : null);
        config.set("role-id", discordRole != null ? discordRole.getIdLong() : null);
    }

    private void loadTeamFromConfig(YamlConfiguration config) {
        this.uuid = UUID.fromString(config.getString("uuid"));
        this.name = config.getString("name");
        this.tag = config.getString("tag");

        String colorCode = config.getString("color-code");
        if (colorCode != null) {
            this.color = ChatColor.getByChar(colorCode);
        } else {
            this.color = DEFAULT_COLOR;
        }

        this.open = config.getBoolean("open");
        this.pvp = config.getBoolean("pvp");

        List<String> playerUUIDs = new ArrayList<>();
        config.getList(name, playerUUIDs);

        for (String playerUUID : playerUUIDs) {
            Player player = null;

            if (playerUUID != null) {
                player = Bukkit.getPlayer(UUID.fromString(playerUUID));
            }

            if (player != null) {
                players.add(player);
                syncAccount(player);
            }
        }

        textChannel = DiscordManager.getTextChannel(config.getLong("text-channel", 0));
        voiceChannel = DiscordManager.getVoiceChannel(config.getLong("voice-channel", 0));
        discordRole = DiscordManager.getRole(config.getLong("role-id", 0));
    }

    public void delete() {
        teams.remove(this);

        if (textChannel != null) {
            textChannel.delete().queue();
        }

        if (voiceChannel != null) {
            voiceChannel.delete().queue();
        }

        if (discordRole != null) {
            discordRole.delete().queue();
        }

        if (teamsFolder != null) {
            File teamFile = new File(teamsFolder, uuid.toString() + ".yml");
            if (teamFile.exists()) {
                teamFile.delete();
            }
        }

        isDeleted = true;
    }

    @Nullable
    public static Team getPlayerTeam(@Nonnull Player player) {
        Objects.requireNonNull(player);
        for (Team team : teams) {
            if (team.getPlayers().contains(player)) {
                return team;
            }
        }

        return null;
    }

    private static List<Team> teams;
    private static File teamsFolder;

    public static void saveTeams() {
        if (teams != null && teamsFolder != null) {
            if (!teamsFolder.exists()) {
                teamsFolder.mkdirs();
            } else {
                // remove team file if it was deleted
                for (File file : teamsFolder.listFiles()) {
                    boolean found = false;
                    for (Team team : teams) {
                        if (file.getName().equals(team.getUuid().toString() + ".yml")) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        file.delete();
                    }
                }
            }

            // add and update team files
            for (Team team : teams) {
                team.saveTeam();
            }
        }
    }

    public static void loadTeams(File teamsFolder) {
        // TODO implement a check system for channels, if they were removed, the config
        // had changed or the server

        Team.teamsFolder = teamsFolder;
        teams = new ArrayList<>();

        if (teamsFolder.exists()) {
            // iterate through all teams and load them
            for (File file : teamsFolder.listFiles()) {
                if (file.getName().endsWith(".yml")) {
                    YamlConfiguration config = new YamlConfiguration();
                    try {
                        config.load(file);
                    } catch (IOException | InvalidConfigurationException e) {
                        e.printStackTrace();
                        continue;
                    }

                    Team team = new Team();
                    team.loadTeamFromConfig(config);

                    teams.add(team);
                }
            }
        }

        teamsFolder.mkdirs();

    }

    public static Team createNewTeam(@Nonnull Player creator, @Nonnull String name, @Nullable String tag) {
        Objects.requireNonNull(creator, "The team creator cannot be null!");

        if (tag == null) {
            tag = name;
        }

        Team team = new Team(UUID.randomUUID(), name, tag);

        team.addPlayer(creator);

        team.color = DEFAULT_COLOR;
        team.open = true;
        team.pvp = true;

        Category teamsCategory = DiscordManager.getTeamsCategory();
        Guild guild = DiscordManager.getServer();

        guild.createRole()
                .setName(team.getName())
                .setColor(getColorValue(team.color))
                .setMentionable(true)
                .queue(role -> {
                    if (role == null) {
                        throw new IllegalStateException("Could not create role for team " + team.getName() + "!");
                    }

                    team.discordRole = role;

                    setChannelPermissions(teamsCategory.createTextChannel(name), role).queue(textChannel -> {
                        if (textChannel == null) {
                            throw new IllegalStateException(
                                    "Could not create text channel for team " + team.getName() + "!");
                        }

                        team.textChannel = textChannel;
                        team.saveTeam();
                    });

                    setChannelPermissions(teamsCategory.createVoiceChannel(name), role).queue(voiceChannel -> {
                        if (voiceChannel == null) {
                            throw new IllegalStateException(
                                    "Could not create voice channel for team " + team.getName() + "!");
                        }

                        team.voiceChannel = voiceChannel;
                        team.saveTeam();
                    });

                    team.saveTeam();
                });

        teams.add(team);
        team.saveTeam();

        return team;
    }

    private static <T extends GuildChannel> ChannelAction<T> setChannelPermissions(@Nonnull ChannelAction<T> channel,
            @Nonnull Role role) {

        channel.addPermissionOverride(DiscordManager.getServer().getPublicRole(), null,
                EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(role, EnumSet.of(Permission.VIEW_CHANNEL), null);

        return channel;
    }

    private static int getColorValue(ChatColor color) {
        switch (color.getChar()) {
            case '0':
                return 0x000000;
            case '1':
                return 0x0000AA;
            case '2':
                return 0x00AA00;
            case '3':
                return 0x00AAAA;
            case '4':
                return 0xAA0000;
            case '5':
                return 0xAA00AA;
            case '6':
                return 0xFFAA00;
            case '7':
                return 0xAAAAAA;
            case '8':
                return 0x555555;
            case '9':
                return 0x5555FF;
            case 'a':
                return 0x55FF55;
            case 'b':
                return 0x55FFFF;
            case 'c':
                return 0xFF5555;
            case 'd':
                return 0xFF55FF;
            case 'e':
                return 0xFFFF55;
            case 'f':
                return 0xFFFFFF;
            default:
                return 0xFFFFFF;
        }
    }

    public static List<Team> getTeams() {
        return List.copyOf(teams);
    }

}
