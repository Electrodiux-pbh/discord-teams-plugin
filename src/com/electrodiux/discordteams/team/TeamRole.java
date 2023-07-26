package com.electrodiux.discordteams.team;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;

import org.bukkit.configuration.file.FileConfiguration;

import com.electrodiux.discordteams.DiscordTeams;
import com.electrodiux.discordteams.Messages;

public enum TeamRole {
    OWNER,
    ADMIN,
    VETERAN,
    MEMBER;

    public static final String ROLE_CONFIG_PATH = "roles";
    public static final int DEFAULT_PRIORITY = 1;

    private int priority = 0;

    private boolean hasAllPermissions = false;
    @Nonnull
    private EnumSet<TeamPermission> permissions;

    @SuppressWarnings("null")
    private TeamRole() {
        this.loadRole();
    }

    public int getPriority() {
        return priority;
    }

    public boolean isHigherPriorityThan(TeamRole other) {
        return this.priority > other.priority;
    }

    public int compare(TeamRole other) {
        return Integer.compare(this.priority, other.priority);
    }

    @Nonnull
    public EnumSet<TeamPermission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(TeamPermission permission) {
        return hasAllPermissions || permissions.contains(permission);
    }

    public boolean hasAllPermissions() {
        return hasAllPermissions;
    }

    @Nonnull
    @SuppressWarnings("null")
    public String roleName() {
        return super.name().toLowerCase();
    }

    public static TeamRole roleOfName(String roleName) {
        return TeamRole.valueOf(roleName.toUpperCase());
    }

    @SuppressWarnings("null")
    private void loadRole() {
        FileConfiguration config = DiscordTeams.getConfiguration();
        String path = ROLE_CONFIG_PATH + "." + roleName();

        this.priority = config.getInt(path + ".priority", DEFAULT_PRIORITY);

        if (config.getBoolean(path + ".all-permissions", false)) {
            this.hasAllPermissions = true;
            this.permissions = EnumSet.allOf(TeamPermission.class);
        } else {
            List<String> permissionStrings = config.getStringList(path + ".permissions");

            this.permissions = EnumSet.noneOf(TeamPermission.class);

            if (permissionStrings != null) {
                for (String permissionString : permissionStrings) {
                    if (permissionString == null)
                        continue;

                    try {
                        TeamPermission permission = TeamPermission.valueOf(permissionString.toUpperCase());
                        this.permissions.add(permission);
                    } catch (IllegalArgumentException e) {
                        DiscordTeams.log(Messages.getMessage("invalid-permission", "%permission%", permissionString));
                    }
                }
            }
        }
    }
}
