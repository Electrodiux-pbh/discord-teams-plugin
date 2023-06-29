package com.electrodiux.discordteams;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.electrodiux.discordteams.discord.AccountLinker;

public class TeamCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
                case "create":
                    if (args.length > 1) {
                        if (sender instanceof Player player) {
                            String teamName = args[1];

                            Bukkit.broadcastMessage(player.getName() + " created a team called " + teamName);
                            DiscordManager.getTeamsCategory().createTextChannel(teamName).queue((channel) -> {
                                sender.sendMessage("The channel for the team '" + teamName
                                        + "' has been created with the ID " + channel.getId());
                            });

                            return true;
                        }
                    }
                    return true;
                case "reload":
                    PluginMain.getConfigManager().reloadConfig();
                    return true;
                case "discordlink":
                    if (sender instanceof Player player) {
                        if (args.length > 1) {
                            AccountLinker.createLink(player, args[1], sender);
                            return true;
                        }
                        return true;
                    } else {
                        Bukkit.getConsoleSender().sendMessage("Cannot execute this command from the console.");
                        return false;
                    }
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // if (args.length == 0) {
        // return List.of("create", "reload", "discordlink");
        // } else {
        // switch (args[0]) {
        // case "discordlink":
        // return List.of("discordusername");
        // }
        // }
        return null;
    }

}
