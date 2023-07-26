package com.electrodiux.discordteams;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.electrodiux.discordteams.discord.LinkVerification;
import com.electrodiux.discordteams.discord.LinkedAccount;
import com.electrodiux.discordteams.team.DiscordTeam;
import com.electrodiux.discordteams.team.TeamMember;

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
                case "rename":
                    return rename(sender, args);
                case "settag":
                    return settag(sender, args);
                case "reload":
                    DiscordTeams.getConfigManager().reloadConfig();
                    return true;
                case "discordlink":
                    return discordlink(sender, args);
                case "discordunlink":
                    return discordunlink(sender, args);
                case "join":
                    return join(sender, args);
                case "leave":
                    return leave(sender, args);
                case "kick":
                    return kick(sender, args);
                default:
                    sender.sendMessage(Messages.getMessage("command.unknown-command", "%command%", args[0]));
                    return true;
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                completions.add("create");
                completions.add("list");
                completions.add("members");
                completions.add("color");
                completions.add("delete");
                completions.add("rename");
                completions.add("settag");
                completions.add("reload");
                completions.add("discordlink");
                completions.add("discordunlink");
                completions.add("join");
                completions.add("leave");
                completions.add("kick");
                return completions;
            } else if (args.length == 2) {
                switch (args[0]) {
                    case "color":
                        List<String> completions = new ArrayList<>();
                        for (ChatColor color : ChatColor.values()) {
                            if (color.isColor()) {
                                completions.add(color.name().toLowerCase());
                            }
                        }
                        return completions;
                    case "join":
                        List<String> completions2 = new ArrayList<>();
                        for (DiscordTeam team : DiscordTeam.getTeams()) {
                            completions2.add(team.getName());
                        }
                        return completions2;
                }

                DiscordTeam team = DiscordTeam.getPlayerTeam(player);
                Bukkit.getConsoleSender().sendMessage("Team " + team);
                if (team != null) {
                    switch (args[0]) {
                        case "kick":
                            List<String> completions = new ArrayList<>();
                            for (TeamMember member : team.getMembers()) {
                                completions.add(String.valueOf(member.getName()));
                            }
                            return completions;
                    }
                }
            }
        }
        return null;
    }

    private boolean join(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 1) {
                String teamName = args[1];
                DiscordTeam team = DiscordTeam.getTeamByName(Objects.requireNonNull(teamName));

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
            TeamMember member = DiscordTeam.getPlayerTeamMember(player);
            if (member != null) {
                DiscordTeam team = member.getTeam();
                team.removeMember(member);
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

    private boolean kick(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 1) {

                DiscordTeam team = DiscordTeam.getPlayerTeam(player);
                if (team != null) {
                    String playerName = args[1];
                    team.kickMember(playerName, player);
                } else {
                    sender.sendMessage(Messages.getMessage("no-team"));
                }
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

                DiscordTeam.createNewTeam(player, teamName, teamTag);
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
            DiscordTeam team = DiscordTeam.getPlayerTeam(player);

            if (team != null) {
                team.delete();
                String msg = Messages.getMessage("team.minecraft.deleted", "%team%", team.getName(), "%team_color%",
                        team.getColor().toString());
                Messages.sendMessage(player, msg);

                team.sendDiscordMessage(
                        Messages.getMessage("team.discord.deleted", player, "%team%", team.getName()));
            } else {
                sender.sendMessage(Messages.getMessage("no-team"));
            }

            return true;
        } else {
            noConsoleCommand();
        }
        return false;
    }

    private boolean rename(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length < 2) {
                sender.sendMessage(Messages.getMessage("no-team-name"));
                return true;
            }

            DiscordTeam team = DiscordTeam.getPlayerTeam(player);
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

            DiscordTeam team = DiscordTeam.getPlayerTeam(player);
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
                DiscordTeam team = DiscordTeam.getPlayerTeam(player);
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
            LinkedAccount account = LinkedAccount.getAccountByMinecraftId(player.getUniqueId());
            if (account != null) {
                LinkedAccount.unregisterAccount(account);
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

        for (DiscordTeam team : DiscordTeam.getTeams()) {
            sb.append("&f- " + team.getColor() + team.getName() + "\n");
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', sb.toString()));

        return true;
    }

    private boolean members(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            DiscordTeam team = DiscordTeam.getPlayerTeam(player);
            if (team != null) {
                StringBuilder sb = new StringBuilder("&aMembers:\n");

                for (TeamMember member : team.getMembers()) {
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
