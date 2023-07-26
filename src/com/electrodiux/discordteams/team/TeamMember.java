package com.electrodiux.discordteams.team;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class TeamMember implements TeamEditor {

    @Nonnull
    private TeamRole role;
    @Nonnull
    private DiscordTeam team;

    @Nonnull
    private OfflinePlayer player;

    protected TeamMember(@Nonnull DiscordTeam team, @Nonnull OfflinePlayer player) {
        this(team, player, DiscordTeam.DEFAULT_ROLE);
    }

    protected TeamMember(@Nonnull DiscordTeam team, @Nonnull UUID playerUniqueId, @Nullable TeamRole role) {
        this(team, getOfflinePlayer(playerUniqueId), role);
    }

    @Nonnull
    private static OfflinePlayer getOfflinePlayer(UUID playerUniqueId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUniqueId);
        if (player == null)
            throw new IllegalArgumentException("Player with UUID " + playerUniqueId + " is not a valid player!");
        return player;
    }

    protected TeamMember(@Nonnull DiscordTeam team, @Nonnull OfflinePlayer player, @Nullable TeamRole role) {
        Objects.requireNonNull(team, "Team cannot be null!");
        Objects.requireNonNull(player, "Player cannot be null!");

        this.team = team;
        this.player = player;

        if (role != null) {
            this.role = role;
        } else {
            this.role = DiscordTeam.DEFAULT_ROLE;
        }
    }

    @Nonnull
    public TeamRole getRole() {
        return role;
    }

    public void setRole(@Nonnull TeamRole role) {
        Objects.requireNonNull(role, "Role cannot be null!");
        this.role = role;
    }

    @Override
    public boolean hasHigherPriorityThan(@Nonnull TeamMember other) {
        return role.isHigherPriorityThan(other.role);
    }

    @Override
    public boolean hasPermission(@Nonnull TeamPermission permission) {
        return role.hasPermission(permission);
    }

    @Nonnull
    public DiscordTeam getTeam() {
        return team;
    }

    @Nonnull
    public OfflinePlayer getOfflinePlayer() {
        return player;
    }

    @Nullable
    public Player getPlayer() {
        return player.getPlayer();
    }

    @Nonnull
    @SuppressWarnings("null")
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Nonnull
    @Override
    public String getName() {
        String name = player.getName();
        return name != null ? name : "(Unknown)";
    }

    @Override
    public void sendMessage(String message) {
        if (isOnline()) {
            Player player = getPlayer();
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    // public void displayTag(@Nullable String tag) {
    // if (isOnline()) {
    // Player player = getPlayer();
    // if (player == null)
    // return;

    // displayTag(player, tag);
    // }
    // }

    // public static void displayTag(@Nonnull Player player, @Nullable String tag) {
    // Objects.requireNonNull(player, "Player cannot be null!");

    // String display = tag != null
    // ? tag + " \u00A7f" + player.getName()
    // : player.getName();

    // player.setPlayerListName(display);
    // player.setDisplayName(display);
    // }

    public boolean isOnline() {
        return player.isOnline();
    }

    @Override
    public int hashCode() {
        return getUniqueId().hashCode();
    }

    @Nullable
    public static TeamMember deserialize(@Nonnull DiscordTeam team, @Nonnull String serialized) {
        Objects.requireNonNull(team, "Team cannot be null!");
        Objects.requireNonNull(serialized, "Serialized cannot be null!");

        String[] split = serialized.split(";");
        if (split.length != 2)
            throw new IllegalArgumentException("Serialized string is not valid!");

        UUID playerUniqueId = UUID.fromString(split[0]);
        TeamRole role = TeamRole.roleOfName(split[1]);

        return new TeamMember(team, Objects.requireNonNull(playerUniqueId), role);
    }

    @Nonnull
    public static String serialize(@Nonnull TeamMember member) {
        Objects.requireNonNull(member, "Member cannot be null!");

        return member.getUniqueId().toString() + ";" + member.getRole().roleName();
    }
}
