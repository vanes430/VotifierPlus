package com.vexsoftware.votifier.commands;

import com.vexsoftware.votifier.VotifierPlusBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class BungeeReloadCommand extends Command {

    private final VotifierPlusBungee plugin;

    public BungeeReloadCommand(VotifierPlusBungee plugin) {
        super("buvotifierplus", "buvotifierplus.admin");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("buvotifierplus.admin")) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "No permission!"));
                return;
            }
            plugin.reload();
            sender.sendMessage(new TextComponent(ChatColor.GREEN + "VotifierPlus reloaded!"));
        } else {
            sender.sendMessage(new TextComponent(ChatColor.YELLOW + "Usage: /buvotifierplus reload"));
        }
    }
}
