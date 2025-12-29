package com.vexsoftware.votifier.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.vexsoftware.votifier.VotifierPlusVelocity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class VelocityReloadCommand implements SimpleCommand {

    private final VotifierPlusVelocity plugin;

    public VelocityReloadCommand(VotifierPlusVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!invocation.source().hasPermission("vevotifierplus.admin")) {
                invocation.source().sendMessage(Component.text("No permission!").color(NamedTextColor.RED));
                return;
            }
            plugin.reload();
            invocation.source().sendMessage(Component.text("VotifierPlus reloaded!").color(NamedTextColor.GREEN));
        } else {
            invocation.source().sendMessage(Component.text("Usage: /vevotifierplus reload").color(NamedTextColor.YELLOW));
        }
    }
}
