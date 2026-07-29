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
package com.vexsoftware.votifier.velocity;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.PublicKey;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.vexsoftware.votifier.common.crypto.RSAIO;
import com.vexsoftware.votifier.common.crypto.RSAKeygen;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class VotifierPlusVelocityCommand implements SimpleCommand {

	private static final String BASE_PERM = "votifierplus";

	private final VotifierPlusVelocity plugin;

	public VotifierPlusVelocityCommand(VotifierPlusVelocity plugin) {
		this.plugin = plugin;
	}

	@Override
	public void execute(Invocation inv) {
		CommandSource src = inv.source();
		String[] args = inv.arguments();

		if (!hasPermission(inv)) {
			src.sendMessage(Component.text("You do not have permission!").color(NamedTextColor.RED));
			return;
		}

		if (args.length == 0) {
			src.sendMessage(Component.text("No valid arguments, see /votifierplus help").color(NamedTextColor.RED));
			return;
		}

		String sub = args[0].toLowerCase();

		switch (sub) {
			case "help" -> help(src);
			case "reload" -> reload(src);
			case "generatekeys" -> generateKeys(src);
			case "test", "vote" -> test(src, args);
			default -> src.sendMessage(
				Component.text("No valid arguments, see /votifierplus help").color(NamedTextColor.RED));
		}
	}

	private void help(CommandSource src) {
		src.sendMessage(Component.text("--- VotifierPlus Help ---").color(NamedTextColor.AQUA));
		src.sendMessage(Component.text("/votifierplus help - Open help page"));
		src.sendMessage(Component.text("/votifierplus reload - Reload the plugin"));
		src.sendMessage(Component.text("/votifierplus generatekeys - Regenerate RSA keys"));
		src.sendMessage(Component.text("/votifierplus test <player> <service> - Test vote connection"));
	}

	private void reload(CommandSource src) {
		if (!src.hasPermission(BASE_PERM + ".reload")) {
			src.sendMessage(Component.text("You do not have permission!").color(NamedTextColor.RED));
			return;
		}
		if (plugin.reload()) {
			src.sendMessage(Component.text("Reloaded VotifierPlus").color(NamedTextColor.AQUA));
		} else {
			src.sendMessage(Component.text("Failed to reload VotifierPlus; check the proxy log.")
					.color(NamedTextColor.RED));
		}
	}

	private void generateKeys(CommandSource src) {
		if (!src.hasPermission(BASE_PERM + ".generatekeys")) {
			src.sendMessage(Component.text("You do not have permission!").color(NamedTextColor.RED));
			return;
		}
		File rsaDirectory = new File(plugin.getDataDirectory() + File.separator + "rsa");
		try {
			for (File f : rsaDirectory.listFiles()) {
				if (!f.isDirectory()) f.delete();
			}
			rsaDirectory.mkdir();
			plugin.setKeyPair(RSAKeygen.generate(2048));
			RSAIO.save(rsaDirectory, plugin.getKeyPair());
		} catch (Exception ex) {
			src.sendMessage(Component.text("Failed to create keys").color(NamedTextColor.RED));
			return;
		}
		src.sendMessage(Component.text("New keys generated").color(NamedTextColor.AQUA));
	}

	private void test(CommandSource src, String[] args) {
		if (!src.hasPermission(BASE_PERM + ".test")) {
			src.sendMessage(Component.text("You do not have permission!").color(NamedTextColor.RED));
			return;
		}
		if (args.length < 3) {
			src.sendMessage(Component.text("Usage: /votifierplus test <player> <service>")
					.color(NamedTextColor.RED));
			return;
		}
		try {
			PublicKey publicKey = plugin.getKeyPair().getPublic();
			String serverIP = plugin.getConfig().getHost();
			int serverPort = plugin.getConfig().getPort();

			String vote = "VOTE\n" + args[2] + "\n" + args[1] + "\nAddress\nTestVote\n";
			SocketAddress addr = new InetSocketAddress(serverIP, serverPort);
			Socket socket = new Socket();
			socket.connect(addr, 1000);
			OutputStream out = socket.getOutputStream();
			out.write(plugin.getVoteReceiver().encrypt(vote.getBytes(), publicKey));
			out.close();
			socket.close();
			src.sendMessage(Component.text("Vote triggered").color(NamedTextColor.AQUA));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean hasPermission(Invocation inv) {
		return inv.source().hasPermission(BASE_PERM + ".admin");
	}
}
