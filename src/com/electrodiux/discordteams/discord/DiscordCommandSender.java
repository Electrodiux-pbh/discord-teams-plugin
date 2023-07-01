package com.electrodiux.discordteams.discord;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class DiscordCommandSender implements RemoteConsoleCommandSender {

    private Player player;
    private MessageChannelUnion channel;

    public DiscordCommandSender(Player player, MessageChannelUnion channel) {
        this.player = player;
        this.channel = channel;
    }

    @Override
    @SuppressWarnings("null")
    public void sendMessage(String arg0) {
        if (arg0 == null)
            return;
        channel.sendMessage(ChatColor.stripColor(arg0)).queue();
    }

    @Override
    @SuppressWarnings("null")
    public void sendMessage(String[] arg0) {
        if (arg0 == null)
            return;

        StringBuilder sb = new StringBuilder();
        for (String s : arg0) {
            sb.append(s + "\n");
        }
        channel.sendMessage(ChatColor.stripColor(sb.toString())).queue();
    }

    // #region wrapper methods

    @Override
    public PermissionAttachment addAttachment(Plugin arg0) {
        return player.addAttachment(arg0);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, int arg1) {
        return player.addAttachment(arg0, arg1);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2) {
        return player.addAttachment(arg0, arg1, arg2);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin arg0, String arg1, boolean arg2, int arg3) {
        return player.addAttachment(arg0, arg1, arg2, arg3);
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return player.getEffectivePermissions();
    }

    @Override
    public boolean hasPermission(String arg0) {
        return player.hasPermission(arg0);
    }

    @Override
    public boolean hasPermission(Permission arg0) {
        return player.hasPermission(arg0);
    }

    @Override
    public boolean isPermissionSet(String arg0) {
        return player.isPermissionSet(arg0);
    }

    @Override
    public boolean isPermissionSet(Permission arg0) {
        return player.isPermissionSet(arg0);
    }

    @Override
    public void recalculatePermissions() {
        player.recalculatePermissions();
    }

    @Override
    public void removeAttachment(PermissionAttachment arg0) {
        player.removeAttachment(arg0);
    }

    @Override
    public boolean isOp() {
        return player.isOp();
    }

    @Override
    public void setOp(boolean arg0) {
        player.setOp(arg0);
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public Server getServer() {
        return player.getServer();
    }

    @Override
    public Spigot spigot() {
        return player.spigot();
    }

    // #endregion

}
