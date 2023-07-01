package com.electrodiux.discordteams;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.electrodiux.discordteams.discord.Account;
import com.electrodiux.discordteams.discord.LinkVerification;

public class TeamCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
                case "create":
                    return create(sender, args);
                case "list":
                    return list(sender, args);
                case "color":
                    return color(sender, args);
                case "delete":
                    return delete(sender, args);
                case "setname":
                    return setname(sender, args);
                case "reload":
                    PluginMain.getConfigManager().reloadConfig();
                    return true;
                case "discordlink":
                    return discordlink(sender, args);
                case "discordunlink":
                    return discordunlink(sender, args);
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

    private boolean create(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 1) {
                String teamName = args[1];
                String teamTag = teamName;

                if (args.length > 2) {
                    teamTag = args[2];
                }

                Team.createNewTeam(player, teamName, teamTag);
                sender.sendMessage(player.getName() + " created a team called " + teamName);

                return true;
            }
        } else {
            noConsoleCommand();
        }
        return false;
    }

    private boolean delete(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            Team team = Team.getPlayerTeam(player);

            if (team != null) {
                team.delete();
            } else {
                sender.sendMessage(Messages.getMessage("no-team"));
            }

            return true;
        } else {
            noConsoleCommand();
        }
        return false;
    }

    private boolean setname(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length < 2) {
                sender.sendMessage(Messages.getMessage("no-team-name"));
                return true;
            }

            Team team = Team.getPlayerTeam(player);
            String newName = args[1];

            if (team != null) {
                team.setName(newName);
            } else {
                sender.sendMessage(Messages.getMessage("no-team"));
            }

            return true;
        } else {
            noConsoleCommand();
        }
        return false;
    }

    private boolean discordlink(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 1) {
                LinkVerification.createLink(player, args[1], sender);
                return true;
            }
            return true;
        } else {
            noConsoleCommand();
            return false;
        }
    }

    private boolean discordunlink(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            Account account = Account.getAccountByMinecraftId(player.getUniqueId());
            if (account != null) {
                Account.unregisterAccount(account);
            } else {
                sender.sendMessage(Messages.getMessage("linking.minecraft.no-discord-account"));
            }

            return true;
        } else {
            noConsoleCommand();
            return false;
        }
    }

    private boolean list(CommandSender sender, String[] args) {
        StringBuilder sb = new StringBuilder("&aTeams:\n");

        // TODO Sort in alphabetical order

        for (Team team : Team.getTeams()) {
            sb.append("&f- &" + team.getColor().getChar() + team.getName() + "\n");
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', sb.toString()));

        return true;
    }

    private boolean color(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 2) {
                String colorName = args[2];

                ChatColor color = ChatColor.valueOf(colorName.toUpperCase());
                if (color != null && color.isColor()) {
                    String teamName = args[1];
                    for (Team team : Team.getTeams()) {
                        if (team.getName().equals(teamName)) {
                            team.setColor(color);
                            break;
                        }
                    }
                } else {
                    sender.sendMessage("Invalid color");
                }
            }
        } else {
            noConsoleCommand();
        }
        return true;
    }

    private void noConsoleCommand() {
        Bukkit.getConsoleSender().sendMessage(Messages.getMessage("no-console-command"));
    }

}
