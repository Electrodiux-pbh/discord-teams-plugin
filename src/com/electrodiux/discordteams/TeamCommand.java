package com.electrodiux.discordteams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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
                case "members":
                    return members(sender, args);
                case "color":
                    return color(sender, args);
                case "delete":
                    return delete(sender, args);
                case "setname":
                    return setname(sender, args);
                case "settag":
                    return settag(sender, args);
                case "reload":
                    PluginMain.getConfigManager().reloadConfig();
                    return true;
                case "discordlink":
                    return discordlink(sender, args);
                case "discordunlink":
                    return discordunlink(sender, args);
                case "join":
                    return join(sender, args);
                case "leave":
                    return leave(sender, args);
                case "syncdiscord":
                    if (sender instanceof Player player) {
                        Team team = Team.getPlayerTeam(player);
                        if (team != null) {
                            Account account = Account.getAccount(player.getUniqueId());
                            Bukkit.getConsoleSender().sendMessage("Account " + account);
                            if (account != null) {
                                team.syncAccount(player.getUniqueId(), account);
                            }
                        } else {
                            sender.sendMessage(Messages.getMessage("no-team"));
                        }
                    } else {
                        noConsoleCommand();
                    }
                    return true;
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("create");
            completions.add("list");
            completions.add("members");
            completions.add("color");
            completions.add("delete");
            completions.add("setname");
            completions.add("settag");
            completions.add("reload");
            completions.add("discordlink");
            completions.add("discordunlink");
            completions.add("join");
            completions.add("leave");
            completions.add("updatedisplay");
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("color")) {
            List<String> completions = new ArrayList<>();
            for (ChatColor color : ChatColor.values()) {
                if (color.isColor()) {
                    completions.add(color.name().toLowerCase());
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }

    private boolean join(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 1) {
                String teamName = args[1];
                Team team = Team.getTeamByName(Objects.requireNonNull(teamName));

                if (team != null) {
                    team.addMember(player);
                    sender.sendMessage(Messages.getMessage("command.team-joined", "%team%", team.getName(),
                            "%team_color%", team.getColor().toString()));
                } else {
                    sender.sendMessage(Messages.getMessage("command.team-not-found", "%team%", teamName));
                }

                return true;
            }
        } else {
            noConsoleCommand();
        }
        return false;
    }

    private boolean leave(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            Team team = Team.getPlayerTeam(player);
            if (team != null) {
                team.removeMember(player);
                sender.sendMessage(Messages.getMessage("command.team-left", "%team%", team.getName(),
                        "%team_color%", team.getColor().toString()));
            } else {
                sender.sendMessage(Messages.getMessage("no-team"));
            }
        } else {
            noConsoleCommand();
        }
        return true;
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
                String msg = Messages.getMessage("team.minecraft.deleted", "%team%", team.getName(), "%team_color%",
                        team.getColor().toString());
                Messages.sendMessage(player, msg);

                team.sendDiscordMessage(
                        Messages.getMessage("team.minecraft.deleted", player, "%team%", team.getName()));
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
                String oldName = team.getName();
                team.setName(newName, player);

                String msg = Messages.getMessage("team.minecraft.name-changed", "%old_name%", oldName, "%new_name%",
                        newName,
                        "%team_color%", team.getColor().toString());
                Messages.sendMessage(player, msg);
            } else {
                sender.sendMessage(Messages.getMessage("no-team"));
            }

            return true;
        } else {
            noConsoleCommand();
        }
        return false;
    }

    private boolean settag(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length < 2) {
                sender.sendMessage(Messages.getMessage("no-team-tag"));
                return true;
            }

            Team team = Team.getPlayerTeam(player);
            String newTag = args[1];

            if (team != null) {
                String oldTag = team.getTag();
                team.setTag(newTag, player);

                String msg = Messages.getMessage("team.minecraft.tag-changed", "%old_tag%", oldTag, "%new_tag%", newTag,
                        "%team_color%", team.getColor().toString());
                Messages.sendMessage(player, msg);
            } else {
                sender.sendMessage(Messages.getMessage("no-team"));
            }

            return true;
        } else {
            noConsoleCommand();
        }
        return false;
    }

    private boolean color(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 1) {
                Team team = Team.getPlayerTeam(player);
                if (team != null) {
                    String colorName = args[1];
                    ChatColor color = null;

                    try {
                        color = ChatColor.valueOf(colorName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Do nothing, color will be null
                    }

                    if (color != null && color.isColor()) {
                        team.setColor(color, player);

                        String msg = Messages.getMessage("team.minecraft.color-changed", "%formated_color%",
                                color + color.name().toLowerCase());
                        Messages.sendMessage(player, msg);
                    } else {
                        sender.sendMessage(Messages.getMessage("invalid-color"));
                    }
                } else {
                    sender.sendMessage(Messages.getMessage("no-team"));
                }
            }
        } else {
            noConsoleCommand();
        }
        return true;
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
            sb.append("&f- " + team.getColor() + team.getName() + "\n");
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', sb.toString()));

        return true;
    }

    private boolean members(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            Team team = Team.getPlayerTeam(player);
            if (team != null) {
                StringBuilder sb = new StringBuilder("&aMembers:\n");

                for (OfflinePlayer member : team.getMembers()) {
                    sb.append("&f- " + member.getName() + "\n");
                }
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', sb.toString()));
            } else {
                sender.sendMessage(Messages.getMessage("no-team"));
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
