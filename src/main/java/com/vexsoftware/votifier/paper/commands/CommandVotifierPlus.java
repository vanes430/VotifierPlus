/*
 * Copyright (C) 2012 Vex Software LLC
 * Optimizations by vanes430.
 * This file is part of Votifier.
 *
 * Votifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Votifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Votifier.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.vexsoftware.votifier.paper.commands;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.vexsoftware.votifier.paper.VotifierPlus;
import com.vexsoftware.votifier.common.crypto.RSAIO;
import com.vexsoftware.votifier.common.crypto.RSAKeygen;

import net.md_5.bungee.api.chat.TextComponent;

public class CommandVotifierPlus implements CommandExecutor {

	private static final String BASE_PERM = "votifierplus";

	private final VotifierPlus plugin;

	public CommandVotifierPlus(VotifierPlus plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(ChatColor.RED + "No valid arguments, see /votifierplus help!");
			return true;
		}

		String sub = args[0].toLowerCase();

		switch (sub) {
			case "help" -> { help(sender); return true; }
			case "reload" -> { return reload(sender); }
			case "generatekeys" -> { return generateKeys(sender); }
			case "test", "vote" -> { return test(sender, args); }
			default -> sender.sendMessage(ChatColor.RED + "No valid arguments, see /votifierplus help!");
		}
		return true;
	}

	private void help(CommandSender sender) {
		ArrayList<TextComponent> msg = new ArrayList<TextComponent>();
		HashMap<String, TextComponent> unsorted = new HashMap<String, TextComponent>();

		addHelpLine(unsorted, "help", "Open help page");
		addHelpLine(unsorted, "reload", "Reload the plugin");
		addHelpLine(unsorted, "generatekeys", "Regenerate votifier keys");
		addHelpLine(unsorted, "test", "Test votifier connection");

		ArrayList<String> sorted = new ArrayList<String>(unsorted.keySet());
		Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
		for (String key : sorted) msg.add(unsorted.get(key));
		sendMessageJson(sender, msg);
	}

	private void addHelpLine(HashMap<String, TextComponent> map, String cmd, String help) {
		String format = plugin.getConfigFile().getHelpLine()
				.replace("%Command%", cmd)
				.replace("%HelpMessage%", help);
		map.put("/votifierplus " + cmd,
			new TextComponent(TextComponent.fromLegacyText(
				net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', format))));
	}

	private void sendMessageJson(CommandSender sender, ArrayList<TextComponent> msg) {
		for (TextComponent comp : msg) sender.spigot().sendMessage(comp);
	}

	private boolean checkPerm(CommandSender sender, String perm) {
		if (!sender.hasPermission(BASE_PERM + "." + perm)) {
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
					plugin.getConfigFile().getFormatNoPerms()));
			return false;
		}
		return true;
	}

	private boolean reload(CommandSender sender) {
		if (!checkPerm(sender, "reload")) return true;
		plugin.reload();
		sender.sendMessage(ChatColor.RED + "VotifierPlus " + plugin.getDescription().getVersion() + " reloaded");
		return true;
	}

	private boolean generateKeys(CommandSender sender) {
		if (!checkPerm(sender, "generatekeys")) return true;
		File rsaDirectory = new File(plugin.getDataFolder() + File.separator + "rsa");
		try {
			for (File f : rsaDirectory.listFiles()) {
				if (!f.isDirectory()) f.delete();
			}
			rsaDirectory.mkdir();
			plugin.setKeyPair(RSAKeygen.generate(2048));
			RSAIO.save(rsaDirectory, plugin.getKeyPair());
		} catch (Exception ex) {
			sender.sendMessage(ChatColor.RED + "Failed to create keys");
			return true;
		}
		sender.sendMessage(ChatColor.RED + "New keys generated");
		return true;
	}

	private boolean test(CommandSender sender, String[] args) {
		if (!checkPerm(sender, "test")) return true;
		if (args.length < 3) {
			sender.sendMessage(ChatColor.RED + "Usage: /votifierplus test <player> <service>");
			return true;
		}
		try {
			PublicKey publicKey = plugin.getKeyPair().getPublic();
			String serverIP = plugin.configFile.getHost();
			int serverPort = plugin.configFile.getPort();
			if (serverIP.length() != 0) {
				String voteString = "VOTE\n" + args[2] + "\n" + args[1] + "\nAddress\nTestVote\n";
				SocketAddress sockAddr = new InetSocketAddress(serverIP, serverPort);
				Socket socket = new Socket();
				socket.connect(sockAddr, 1000);
				OutputStream out = socket.getOutputStream();
				out.write(plugin.getVoteReceiver().encrypt(voteString.getBytes(), publicKey));
				out.close();
				socket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		sender.sendMessage(ChatColor.RED + "Check console for test results");
		return true;
	}
}
