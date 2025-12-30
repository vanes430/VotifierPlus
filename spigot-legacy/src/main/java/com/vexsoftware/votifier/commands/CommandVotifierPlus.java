package com.vexsoftware.votifier.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import com.vexsoftware.votifier.VotifierPlus;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class CommandVotifierPlus implements CommandExecutor, TabCompleter {

    private final VotifierPlus plugin;

    public CommandVotifierPlus(VotifierPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("votifierplus.reload") && !sender.hasPermission("votifierplus.admin")) {
                    sender.sendMessage(plugin.getConfigFile().getFormatNoPerms());
                    return true;
                }
                plugin.reload();
                sender.sendMessage("§aVotifierPlus reloaded!");
                return true;
            } else if (args[0].equalsIgnoreCase("test")) {
                if (!sender.hasPermission("votifierplus.test") && !sender.hasPermission("votifierplus.admin")) {
                    sender.sendMessage(plugin.getConfigFile().getFormatNoPerms());
                    return true;
                }
                String username = (args.length > 1) ? args[1] : "TestUser";
                String service = (args.length > 2) ? args[2] : "TestService";
                
                Vote vote = new Vote();
                vote.setUsername(username);
                vote.setServiceName(service);
                vote.setAddress("127.0.0.1");
                vote.setTimeStamp(String.valueOf(System.currentTimeMillis()));
                
                plugin.getServer().getPluginManager().callEvent(new VotifierEvent(vote));
                sender.sendMessage("§aTest vote sent for user: " + username);
                return true;
            } else if (args[0].equalsIgnoreCase("check")) {
                if (!sender.hasPermission("votifierplus.check") && !sender.hasPermission("votifierplus.admin")) {
                    sender.sendMessage(plugin.getConfigFile().getFormatNoPerms());
                    return true;
                }
                if (plugin.getWaitingList().isEmpty()) {
                    sender.sendMessage("§eWaiting list is empty.");
                } else {
                    sender.sendMessage("§6Waiting List:");
                    plugin.getWaitingList().forEach((user, votes) -> {
                        for (Vote vote : votes) {
                            sender.sendMessage("§7- §b" + user + " §7from §e" + vote.getServiceName());
                        }
                    });
                }
                return true;
            } else if (args[0].equalsIgnoreCase("clear")) {
                if (!sender.hasPermission("votifierplus.clear") && !sender.hasPermission("votifierplus.admin")) {
                    sender.sendMessage(plugin.getConfigFile().getFormatNoPerms());
                    return true;
                }
                plugin.getWaitingList().clear();
                sender.sendMessage("§aWaiting list cleared.");
                return true;
            }
        }
        
        sender.sendMessage("§bVotifierPlus §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7/votifierplus reload");
        sender.sendMessage("§7/votifierplus test [user] [service]");
        sender.sendMessage("§7/votifierplus check");
        sender.sendMessage("§7/votifierplus clear");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("votifierplus.reload") || sender.hasPermission("votifierplus.admin")) options.add("reload");
            if (sender.hasPermission("votifierplus.test") || sender.hasPermission("votifierplus.admin")) options.add("test");
            if (sender.hasPermission("votifierplus.check") || sender.hasPermission("votifierplus.admin")) options.add("check");
            if (sender.hasPermission("votifierplus.clear") || sender.hasPermission("votifierplus.admin")) options.add("clear");
            StringUtil.copyPartialMatches(args[0], options, completions);
        }
        return completions;
    }
}