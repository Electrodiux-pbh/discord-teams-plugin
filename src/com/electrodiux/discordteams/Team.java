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

    @Nonnull
    public static final ChatColor DEFAULT_COLOR = ChatColor.WHITE;

    private transient boolean isDeleted;

    @Nonnull
    private UUID uuid;
    @Nonnull
    private String name = "";
    @Nonnull
    private String tag = "";

    @Nonnull
    private ChatColor color;

    @Nonnull
    private List<Player> members;

    private boolean open;
    private boolean pvp;

    // discord channels
    private TextChannel textChannel;
    private VoiceChannel voiceChannel;
    private Role discordRole;

    // empty constructor for loading the configuration
    @SuppressWarnings("null")
    private Team() {
        this.members = new ArrayList<>();
        this.color = DEFAULT_COLOR;
        uuid = UUID.randomUUID();
    }

    private Team(@Nonnull UUID uuid, @Nonnull String name, @Nonnull String tag) {
        this();
        this.uuid = uuid;
        this.name = name;
        this.tag = tag;
    }

    @Nonnull
    public UUID getUuid() {
        return uuid;
    }

    @Nonnull
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

    @Nonnull
    public String getTag() {
        return tag;
    }

    public void setTag(@Nonnull String tag) {
        this.tag = tag;

        updateTagDisplayToPlayers();

        saveTeam();
    }

    public List<Player> getMembers() {
        return members;
    }

    public void addMember(@Nonnull Player player) {
        this.members.add(player);

        displayTagToPlayer(player, getColoredTag());
        syncAccount(player);

        saveTeam();
    }

    public void removeMember(@Nonnull Player player) {
        this.members.remove(player);

        Team.displayTagToPlayer(player, null);
        syncAccount(player);

        if (this.members.size() <= 0) {
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

    @Nonnull
    public ChatColor getColor() {
        return color;
    }

    public void setColor(@Nonnull ChatColor color) {
        if (color.isColor()) {
            this.color = color;
        } else {
            this.color = DEFAULT_COLOR;
        }

        updateTagDisplayToPlayers();
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
        for (Player player : members) {
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
        Bukkit.getConsoleSender().sendMessage("Syncing account for " + player.getName());
        Account account = Account.getAccount(player);
        if (account != null) {
            syncAccount(player, account);
        }
    }

    @SuppressWarnings("null")
    private void syncAccount(@Nonnull Player player, @Nonnull Account account) {
        User discordUser = account.getDiscordUser();
        Bukkit.getConsoleSender().sendMessage("Discord user " + discordUser);
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

    public String getColoredTag() {
        return getColor() + tag;
    }

    public static void updatePlayerDisplayTag(@Nonnull Player player) {
        Team team = getPlayerTeam(player);
        Team.displayTagToPlayer(player, team != null ? team.getColoredTag() : null);
    }

    public void updateTagDisplayToPlayers() {
        displayTagToPlayers(members, getColoredTag());
    }

    private static void displayTagToPlayers(@Nonnull List<Player> players, @Nullable String tag) {
        for (Player player : players) {
            if (player != null) {
                displayTagToPlayer(player, tag);
            }
        }
    }

    private static void displayTagToPlayer(@Nonnull Player player, @Nullable String tag) {
        String display = tag != null
                ? tag + " \u00A7f" + player.getName()
                : player.getName();

        player.setPlayerListName(display);
        player.setDisplayName(display);
    }

    public boolean containsPlayer(Player player) {
        return members.contains(player);
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

        String[] playerUUIDs = new String[members.size()];
        int i = 0;
        for (Player player : members) {
            playerUUIDs[i] = player.getUniqueId().toString();
            i++;
        }

        config.set("players", playerUUIDs);

        config.set("text-channel", textChannel != null ? textChannel.getIdLong() : null);
        config.set("voice-channel", voiceChannel != null ? voiceChannel.getIdLong() : null);
        config.set("role-id", discordRole != null ? discordRole.getIdLong() : null);
    }

    @SuppressWarnings("null")
    private void loadTeamFromConfig(YamlConfiguration config) {
        String uuidString = config.getString("uuid");
        String name = config.getString("name");
        String tag = config.getString("tag");
        Objects.requireNonNull(uuidString, "Team UUID cannot be null");
        Objects.requireNonNull(name, "Team name cannot be null");
        Objects.requireNonNull(tag, "Team tag cannot be null");

        this.uuid = UUID.fromString(uuidString);
        this.name = name;
        this.tag = tag;

        String colorCode = config.getString("color-code");
        if (colorCode != null) {
            ChatColor color = ChatColor.getByChar(colorCode);
            this.color = color != null ? color : DEFAULT_COLOR;
        } else {
            this.color = DEFAULT_COLOR;
        }

        this.open = config.getBoolean("open");
        this.pvp = config.getBoolean("pvp");

        List<String> playerUUIDs = config.getStringList("players");

        for (String playerUUID : playerUUIDs) {
            Player player = null;

            if (playerUUID != null) {
                player = Bukkit.getPlayer(UUID.fromString(playerUUID));
            }

            if (player != null) {
                members.add(player);
                syncAccount(player);
            }
        }

        textChannel = DiscordManager.getTextChannel(config.getLong("text-channel", 0));
        voiceChannel = DiscordManager.getVoiceChannel(config.getLong("voice-channel", 0));
        discordRole = DiscordManager.getRole(config.getLong("role-id", 0));

        updateTagDisplayToPlayers();
    }

    public void delete() {
        isDeleted = true;

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

        Team.displayTagToPlayers(members, null);
    }

    @Nullable
    public static Team getPlayerTeam(@Nonnull Player player) {
        Objects.requireNonNull(player);
        for (Team team : teams) {
            for (Player member : team.getMembers()) {
                if (member.getUniqueId().equals(player.getUniqueId())) {
                    return team;
                }
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

                    if (team.members.isEmpty()) {
                        team.delete();
                        continue;
                    }

                    teams.add(team);
                }
            }
        }

        teamsFolder.mkdirs();

    }

    @SuppressWarnings("null")
    public static Team createNewTeam(@Nonnull Player creator, @Nonnull String name, @Nullable String tag) {
        Objects.requireNonNull(creator);
        Objects.requireNonNull(name);

        if (tag == null) {
            tag = name;
        }

        name = ChatColor.stripColor(name);
        tag = ChatColor.stripColor(tag);

        Team team = new Team(UUID.randomUUID(), name, tag);

        team.addMember(creator);

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

                    setChannelPermissions(teamsCategory.createTextChannel(team.getName()), role).queue(textChannel -> {
                        if (textChannel == null) {
                            throw new IllegalStateException(
                                    "Could not create text channel for team " + team.getName() + "!");
                        }

                        team.textChannel = textChannel;
                        team.saveTeam();
                    });

                    setChannelPermissions(teamsCategory.createVoiceChannel(team.getName()), role)
                            .queue(voiceChannel -> {
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
                return 0x010101; // Color with value 0 is means no color, so we use a very dark gray instead
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
