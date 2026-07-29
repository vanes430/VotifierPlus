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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class VotifierPlusTabCompleter implements TabCompleter {

	private static final String[] ROOT_COMPLETIONS = { "help", "reload", "generatekeys", "test" };

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (args.length <= 1) {
			return filterCompletions(ROOT_COMPLETIONS, args.length == 0 ? "" : args[0]);
		}

		if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
			return getPlayerNames(args[1]);
		}

		if (args.length == 3 && args[0].equalsIgnoreCase("test")) {
			return Collections.singletonList("VoteService");
		}

		return Collections.emptyList();
	}

	private List<String> filterCompletions(String[] options, String prefix) {
		List<String> result = new ArrayList<String>();
		for (String opt : options) {
			if (prefix.isEmpty() || opt.regionMatches(true, 0, prefix, 0, prefix.length())) {
				result.add(opt);
			}
		}
		Collections.sort(result);
		return result;
	}

	private List<String> getPlayerNames(String prefix) {
		List<String> names = new ArrayList<String>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			String name = player.getName();
			if (prefix.isEmpty() || name.regionMatches(true, 0, prefix, 0, prefix.length())) {
				names.add(name);
			}
		}
		Collections.sort(names);
		return names;
	}
}
