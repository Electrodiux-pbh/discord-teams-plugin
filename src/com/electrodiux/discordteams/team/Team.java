package com.electrodiux.discordteams.team;

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

import com.electrodiux.discordteams.DiscordManager;
import com.electrodiux.discordteams.DiscordTeams;
import com.electrodiux.discordteams.Messages;
import com.electrodiux.discordteams.discord.LinkedAccount;

import net.dv8tion.jda.api.Permission;
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
    @Nonnull
    public static final TeamRole DEFAULT_ROLE = TeamRole.MEMBER;

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
    private List<TeamMember> members;

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

    public List<TeamMember> getMembers() {
        return members;
    }

    @SuppressWarnings("null")
    public void addMember(@Nonnull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        try {
            // Sending messages first to prevent player from receiving the message
            sendMinecraftMessage(Messages.getMessage("team.minecraft.player-joined", "%player%", player.getName()));
            sendDiscordMessage(Messages.getMessage("team.discord.player-joined", "%player%", player.getName()));

            TeamMember member = this.createTeamMember(player);
            this.members.add(member);

            member.displayTag(getColoredTag());
            syncAccount(player.getUniqueId());
        } finally {
            saveTeam();
        }
    }

    private TeamMember createTeamMember(@Nonnull OfflinePlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");
        return new TeamMember(this, player, DEFAULT_ROLE);
    }

    public void kickMember(@Nonnull String name, @Nonnull CommandSender kicker) {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(kicker, "Kicker cannot be null");

        try {
            TeamMember member = null;

            for (Iterator<TeamMember> i = members.iterator(); i.hasNext();) {
                TeamMember teamMember = i.next();
                if (name.equals(String.valueOf(teamMember.getName()))) { // Important, name is not null but
                                                                         // member.getName()
                    // can be, so we do not use
                    // "member.getName().equals(name)"
                    i.remove();
                    member = teamMember;
                    break;
                }
            }

            if (member == null) {
                kicker.sendMessage(Messages.getMessage("command.player-not-found", "%player%", name));
                return;
            }

            // Sending messages after removing player for preventing receiving the message
            // and check if the player really was removed
            sendMinecraftMessage(Messages.getMessage("team.minecraft.player-kicked", "%player%", name,
                    "%kicker%", kicker.getName()));
            sendDiscordMessage(Messages.getMessage("team.discord.player-kicked", "%player%", name,
                    "%kicker%", kicker.getName()));

            member.displayTag(null);
        } finally {
            saveTeam();
        }
    }

    @SuppressWarnings("null")
    public void removeMember(@Nonnull TeamMember member) {
        Objects.requireNonNull(member, "Player cannot be null");

        try {
            boolean removed = false;
            for (Iterator<TeamMember> i = members.iterator(); i.hasNext();) {
                TeamMember teamMember = i.next();
                if (teamMember.getUniqueId().equals(member.getUniqueId())) {
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
                    Messages.getMessage("team.minecraft.player-left", "%player%", member.getName()));
            sendDiscordMessage(Messages.getMessage("team.discord.player-left", "%player%", member.getName()));

            member.displayTag(null);
            syncAccount(member.getUniqueId());

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
        for (TeamMember member : members) {
            member.sendMessage(message);
        }
    }

    @SuppressWarnings("null")
    public void sendMessage(@Nonnull String message) {
        sendDiscordMessage(ChatColor.stripColor(message));
        sendMinecraftMessage(message);
    }

    public static void syncAccount(@Nonnull LinkedAccount account) {
        UUID playerUuid = account.getPlayerUniqueId();

        TeamMember member = getPlayerTeamMember(playerUuid);
        if (member != null) {
            member.getTeam().syncAccount(playerUuid, account);
        }
    }

    private void syncAccount(@Nonnull UUID playerUuid) {
        LinkedAccount account = LinkedAccount.getAccount(playerUuid);
        if (account != null) {
            syncAccount(playerUuid, account);
        }
    }

    @SuppressWarnings("null")
    public void syncAccount(@Nonnull UUID playerUuid, @Nonnull LinkedAccount account) {
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
        TeamMember member = getPlayerTeamMember(player);
        if (member != null) {
            member.displayTag(member.getTeam().getColoredTag());
        }
        TeamMember.displayTag(player, null);
    }

    public void updateTagDisplayToPlayers() {
        displayTagToPlayers(members, getColoredTag());
    }

    private static void displayTagToPlayers(@Nonnull List<TeamMember> players, @Nullable String tag) {
        for (TeamMember member : players) {
            member.displayTag(tag);
        }
    }

    public boolean containsPlayer(Player player) {
        return containsPlayer(player.getUniqueId());
    }

    public boolean containsPlayer(UUID playerUuid) {
        for (TeamMember member : members) {
            if (member.getUniqueId().equals(playerUuid)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @SuppressWarnings("null")
    public TeamMember getMember(@Nonnull Player player) {
        return getMember(player.getUniqueId());
    }

    @Nullable
    public TeamMember getMember(@Nonnull UUID playerUuid) {
        for (TeamMember member : members) {
            if (member.getUniqueId().equals(playerUuid)) {
                return member;
            }
        }
        return null;
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

        String[] teamMembers = new String[members.size()];
        int i = 0;
        for (TeamMember member : members) {
            if (member == null) {
                continue;
            }

            teamMembers[i] = TeamMember.serialize(member);
            i++;
        }

        config.set("players", teamMembers);

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
        List<String> teamMembers = config.getStringList("players");
        for (String memberString : teamMembers) {
            if (memberString != null) {
                TeamMember member = TeamMember.deserialize(this, memberString);

                members.add(member);
                syncAccount(member.getUniqueId());
            }
        }
        // ---------------

        textChannel = DiscordManager.getTextChannel(config.getLong("text-channel", 0));
        voiceChannel = DiscordManager.getVoiceChannel(config.getLong("voice-channel", 0));
        discordRole = DiscordManager.getRole(config.getLong("role-id", 0));

        // update discord
        updateDiscord();

        updateTagDisplayToPlayers();
    }

    // #region Discord

    private void updateDiscord() {
        boolean newRole = false;
        boolean newTextChannel = false;
        boolean newVoiceChannel = false;

        if (discordRole != null) {
            discordRole.getManager().setColor(getColorHexadecimal(getColor())).queue();
            discordRole.getManager().setName(getName()).queue();
        } else {
            createRole();
            newRole = true;
        }

        if (textChannel != null) {
            textChannel.getManager().setName(getName()).queue();
        } else {
            createTextChannel();
            newTextChannel = true;
        }

        if (voiceChannel != null) {
            voiceChannel.getManager().setName(getName()).queue();
        } else {
            createVoiceChannel();
            newVoiceChannel = true;
        }

        if (newRole) {
            sendDiscordMessage(Messages.getMessage("team.discord.new-role"));
        }

        if (newTextChannel) {
            sendDiscordMessage(Messages.getMessage("team.discord.new-text-channel"));
        }

        if (newVoiceChannel) {
            sendDiscordMessage(Messages.getMessage("team.discord.new-voice-channel"));
        }

        saveTeam();
    }

    private TextChannel createTextChannel() {
        Category teamsCategory = DiscordManager.getTeamsCategory();

        textChannel = setChannelPermissions(teamsCategory.createTextChannel(getName()), discordRole).complete();
        if (textChannel == null) {
            throw new IllegalStateException(
                    "Could not create text channel for team " + getName() + "!");
        }

        return textChannel;
    }

    private VoiceChannel createVoiceChannel() {
        Category teamsCategory = DiscordManager.getTeamsCategory();

        voiceChannel = setChannelPermissions(teamsCategory.createVoiceChannel(getName()), discordRole).complete();
        if (voiceChannel == null) {
            throw new IllegalStateException(
                    "Could not create voice channel for team " + getName() + "!");
        }

        return voiceChannel;
    }

    private static <T extends GuildChannel> ChannelAction<T> setChannelPermissions(@Nonnull ChannelAction<T> channel,
            @Nullable Role role) {

        channel.addPermissionOverride(DiscordManager.getServer().getPublicRole(), null,
                EnumSet.of(Permission.VIEW_CHANNEL));

        if (role != null) {
            channel.addPermissionOverride(role, EnumSet.of(Permission.VIEW_CHANNEL), null);
        }

        return channel;
    }

    private Role createRole() {
        discordRole = DiscordManager.getServer().createRole()
                .setName(getName())
                .setColor(getColorHexadecimal(getColor()))
                .setMentionable(true).complete();

        if (discordRole == null) {
            throw new IllegalStateException(
                    "Could not create role for team " + getName() + "!");
        }

        if (textChannel != null) {
            textChannel.getManager().putRolePermissionOverride(discordRole.getIdLong(),
                    EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
        }

        if (voiceChannel != null) {
            voiceChannel.getManager().putRolePermissionOverride(discordRole.getIdLong(),
                    EnumSet.of(Permission.VIEW_CHANNEL), null).queue();
        }

        return discordRole;
    }

    // #endregion

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

        TeamMember member = getPlayerTeamMember(player);

        return member != null ? member.getTeam() : null;
    }

    @Nullable
    public static TeamMember getPlayerTeamMember(@Nonnull Player player) {
        Objects.requireNonNull(player, "Player cannot be null");

        return getPlayerTeamMember(player.getUniqueId());
    }

    @Nullable
    public static TeamMember getPlayerTeamMember(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");

        for (Team team : teams) {
            for (TeamMember member : team.getMembers()) {
                if (member.getUniqueId().equals(uuid)) {
                    return member;
                }
            }
        }

        return null;
    }

    @Override
    public int hashCode() {
        return teamUuid.hashCode();
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

        team.createRole();
        team.createTextChannel();
        team.createVoiceChannel();

        teams.add(team);
        team.saveTeam();

        return team;
    }

    private static int getColorHexadecimal(ChatColor color) {
        return DiscordTeams.getConfiguration().getInt("discord.colors." + color.name().toLowerCase(), 0);
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
