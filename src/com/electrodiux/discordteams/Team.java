package com.electrodiux.discordteams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
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
    public static final boolean DEFAULT_PVP = true;
    public static final boolean DEFAULT_OPEN = false;

    private transient boolean isDeleted;

    @Nonnull
    private UUID teamUuid;
    @Nonnull
    private String name = "";
    @Nonnull
    private String tag = "";

    @Nonnull
    private ChatColor color;

    @Nonnull
    private List<OfflinePlayer> members;

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
        teamUuid = UUID.randomUUID();
    }

    private Team(@Nonnull UUID uuid, @Nonnull String name, @Nonnull String tag) {
        this();
        this.teamUuid = uuid;
        this.name = name;
        this.tag = tag;
    }

    @Nonnull
    public UUID getTeamUuid() {
        return teamUuid;
    }

    @Nonnull
    private String getByMessage(@Nullable CommandSender changer) {
        return changer != null ? Messages.getMessage("team.discord.by", changer) : "";
    }

    @Nonnull
    public String getName() {
        return name;
    }

    // TODO Create team changed event to better handling of things

    public void setName(@Nonnull String newName, @Nullable CommandSender changer) {
        try {
            String oldName = this.name;
            this.name = newName;

            sendDiscordMessage(Messages.getMessage("team.discord.name-changed", "%old_name%", oldName,
                    "%new_name%", newName, "%by%", getByMessage(changer)));

            if (discordRole != null) {
                discordRole.getManager().setName(newName).queue();
            }

            if (textChannel != null) {
                textChannel.getManager().setName(newName).queue();
            }

            if (voiceChannel != null) {
                voiceChannel.getManager().setName(newName).queue();
            }
        } finally {
            saveTeam();
        }
    }

    @Nonnull
    public String getTag() {
        return tag;
    }

    public void setTag(@Nonnull String newTag, @Nullable CommandSender changer) {
        try {
            String oldTag = this.tag;
            this.tag = newTag;

            sendDiscordMessage(Messages.getMessage("team.discord.tag-changed", "%old_tag%", oldTag,
                    "%new_tag%", newTag, "%by%", getByMessage(changer)));

            updateTagDisplayToPlayers();
        } finally {
            saveTeam();
        }
    }

    @Nonnull
    public ChatColor getColor() {
        return color;
    }

    public void setColor(@Nonnull ChatColor newColor, @Nullable CommandSender changer) {
        try {
            if (newColor.isColor()) {
                this.color = newColor;
            } else {
                this.color = DEFAULT_COLOR;
            }

            sendDiscordMessage(Messages.getMessage("team.discord.color-changed",
                    "%color%", color.name().toLowerCase(), "%by%", getByMessage(changer)));

            updateTagDisplayToPlayers();
            if (discordRole != null) {
                discordRole.getManager().setColor(getColorHexadecimal(newColor)).queue();
            }
        } finally {
            saveTeam();
        }
    }

    public List<OfflinePlayer> getMembers() {
        return members;
    }

    @SuppressWarnings("null")
    public void addMember(@Nonnull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        try {
            // Sending messages first to prevent player from receiving the message
            sendMinecraftMessage(Messages.getMessage("team.minecraft.player-joined", "%player%", player.getName()));
            sendDiscordMessage(Messages.getMessage("team.discord.player-joined", "%player%", player.getName()));

            this.members.add(player);

            Team.displayTagToPlayer(player, getColoredTag());
            syncAccount(player.getUniqueId());
        } finally {
            saveTeam();
        }
    }

    @SuppressWarnings("null")
    public void removeMember(@Nonnull OfflinePlayer offlinePlayer) {
        Objects.requireNonNull(offlinePlayer, "Player cannot be null");

        try {
            boolean removed = false;
            for (Iterator<OfflinePlayer> i = members.iterator(); i.hasNext();) {
                OfflinePlayer member = i.next();
                if (member.getUniqueId().equals(offlinePlayer.getUniqueId())) {
                    i.remove();
                    removed = true;
                    break;
                }
            }

            if (!removed) {
                return;
            }

            // Sending messages after removing player for preventing receiving the message
            // and check if the player really was removed
            sendMinecraftMessage(
                    Messages.getMessage("team.minecraft.player-left", "%player%", offlinePlayer.getName()));
            sendDiscordMessage(Messages.getMessage("team.discord.player-left", "%player%", offlinePlayer.getName()));

            Player player = offlinePlayer.getPlayer();
            if (player != null) {
                Team.displayTagToPlayer(player, null);
            }
            syncAccount(offlinePlayer.getUniqueId());

            if (this.members.size() <= 0) {
                delete();
                return;
            }
        } finally {
            saveTeam();
        }
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
        for (OfflinePlayer offlinePlayer : members) {
            if (offlinePlayer.isOnline()) {
                Player player = offlinePlayer.getPlayer();
                if (player != null) {
                    player.sendMessage(message);
                }
            }
        }
    }

    @SuppressWarnings("null")
    public void sendMessage(@Nonnull String message) {
        sendDiscordMessage(ChatColor.stripColor(message));
        sendMinecraftMessage(message);
    }

    public static void syncAccount(@Nonnull Account account) {
        UUID playerUuid = account.getPlayerUniqueId();

        Team team = getPlayerTeam(playerUuid);
        if (team != null) {
            team.syncAccount(playerUuid, account);
        }
    }

    private void syncAccount(@Nonnull UUID playerUuid) {
        Account account = Account.getAccount(playerUuid);
        if (account != null) {
            syncAccount(playerUuid, account);
        }
    }

    @SuppressWarnings("null")
    public void syncAccount(@Nonnull UUID playerUuid, @Nonnull Account account) {
        User discordUser = account.getDiscordUser();

        Bukkit.getConsoleSender().sendMessage("Discord user: " + discordUser);

        if (discordUser == null) {
            return;
        }

        Bukkit.getConsoleSender().sendMessage("Role: " + discordRole);

        if (discordRole != null) {
            if (containsPlayer(playerUuid)) {
                Bukkit.getConsoleSender().sendMessage("Adding role to " + discordUser.getName());
                DiscordManager.getServer().addRoleToMember(discordUser, discordRole).queue();
            } else {
                Bukkit.getConsoleSender().sendMessage("Removing role from " + discordUser.getName());
                DiscordManager.getServer().removeRoleFromMember(discordUser,
                        discordRole).queue();
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

    private static void displayTagToPlayers(@Nonnull List<OfflinePlayer> players, @Nullable String tag) {
        for (OfflinePlayer offlinePlayer : players) {
            if (offlinePlayer.isOnline()) {
                Player player = offlinePlayer.getPlayer();
                if (player != null) {
                    displayTagToPlayer(player, tag);
                }
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
        return containsPlayer(player.getUniqueId());
    }

    public boolean containsPlayer(UUID playerUuid) {
        for (OfflinePlayer offlinePlayer : members) {
            if (offlinePlayer.getUniqueId().equals(playerUuid)) {
                return true;
            }
        }
        return false;
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
            File teamFile = new File(teamsFolder, teamUuid.toString() + ".yml");
            if (!teamFile.exists()) {
                teamFile.createNewFile();
            }
            config.save(teamFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveTeamToConfig(YamlConfiguration config) {
        config.set("uuid", teamUuid.toString());
        config.set("name", name);
        config.set("tag", tag);

        config.set("color-code", color != null ? color.getChar() : DEFAULT_COLOR.getChar());

        config.set("open", open);
        config.set("pvp", pvp);

        String[] playerUUIDs = new String[members.size()];
        int i = 0;
        for (OfflinePlayer offlinePlayer : members) {
            playerUUIDs[i] = offlinePlayer.getUniqueId().toString();
            i++;
        }

        config.set("players", playerUUIDs);

        config.set("text-channel", textChannel != null ? textChannel.getIdLong() : null);
        config.set("voice-channel", voiceChannel != null ? voiceChannel.getIdLong() : null);
        config.set("role-id", discordRole != null ? discordRole.getIdLong() : null);
    }

    @SuppressWarnings("null")
    private void loadTeamFromConfig(YamlConfiguration config) {
        // ----CORE PROPERTIES----
        String uuidString = config.getString("uuid");
        Objects.requireNonNull(uuidString, "Team UUID cannot be null");
        this.teamUuid = UUID.fromString(uuidString);

        String name = config.getString("name");
        Objects.requireNonNull(name, "Team name cannot be null");
        this.name = name;

        String tag = config.getString("tag");
        Objects.requireNonNull(tag, "Team tag cannot be null");
        this.tag = tag;
        // ------------------------

        String colorCode = config.getString("color-code");
        if (colorCode != null) {
            ChatColor color = ChatColor.getByChar(colorCode);
            this.color = color != null ? color : DEFAULT_COLOR;
        } else {
            this.color = DEFAULT_COLOR;
        }

        this.open = config.getBoolean("open", DEFAULT_OPEN);
        this.pvp = config.getBoolean("pvp", DEFAULT_PVP);

        // ----PLAYERS----
        List<String> playerUUIDs = config.getStringList("players");
        for (String playerUUID : playerUUIDs) {
            if (playerUUID != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUUID));

                members.add(offlinePlayer);
                syncAccount(offlinePlayer.getUniqueId());
            }
        }
        // ---------------

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
            File teamFile = new File(teamsFolder, teamUuid.toString() + ".yml");
            if (teamFile.exists()) {
                teamFile.delete();
            }
        }

        Team.displayTagToPlayers(members, null);
    }

    @Nullable
    public static Team getPlayerTeam(@Nonnull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        return getPlayerTeam(player.getUniqueId());
    }

    @Nullable
    public static Team getPlayerTeam(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");

        for (Team team : teams) {
            for (OfflinePlayer member : team.getMembers()) {
                if (member.getUniqueId().equals(uuid)) {
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
                        if (file.getName().equals(team.getTeamUuid().toString() + ".yml")) {
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
                .setColor(getColorHexadecimal(team.color))
                .setMentionable(true)
                .queue(role -> {
                    if (role == null) {
                        throw new IllegalStateException("Could not create role for team " + team.getName() + "!");
                    }

                    try {
                        team.discordRole = role;

                        for (OfflinePlayer member : team.getMembers()) {
                            team.syncAccount(member.getUniqueId());
                        }

                        setChannelPermissions(teamsCategory.createTextChannel(team.getName()), role)
                                .queue(textChannel -> {
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
                    } finally {
                        team.saveTeam();
                    }
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

    private static int getColorHexadecimal(ChatColor color) {
        switch (color) {
            case BLACK:
                return 0x010101; // Color with value 0 is means no color, so we use a very dark gray instead
            case DARK_BLUE:
                return 0x0000AA;
            case DARK_GREEN:
                return 0x00AA00;
            case DARK_AQUA:
                return 0x00AAAA;
            case DARK_RED:
                return 0xAA0000;
            case DARK_PURPLE:
                return 0xAA00AA;
            case GOLD:
                return 0xFFAA00;
            case GRAY:
                return 0xAAAAAA;
            case DARK_GRAY:
                return 0x555555;
            case BLUE:
                return 0x5555FF;
            case GREEN:
                return 0x55FF55;
            case AQUA:
                return 0x55FFFF;
            case RED:
                return 0xFF5555;
            case LIGHT_PURPLE:
                return 0xFF55FF;
            case YELLOW:
                return 0xFFFF55;
            case WHITE:
                return 0xFFFFFF;
            default:
                return 0xFFFFFF;
        }
    }

    public static List<Team> getTeams() {
        return List.copyOf(teams);
    }

    @Nullable
    public static Team getTeamByName(@Nonnull String teamName) {
        for (Team team : teams) {
            if (team.getName().equalsIgnoreCase(teamName)) {
                return team;
            }
        }
        return null;
    }

}
