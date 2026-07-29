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
package com.vexsoftware.votifier.paper.vote;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.paper.VotifierPlus;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PendingVoteManager implements Listener {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type TYPE = new TypeToken<Map<String, List<Vote>>>() {}.getType();
	private static final String FILE_NAME = "pending_votes.json";

	private final VotifierPlus plugin;
	private final File file;
	private final int delayTicks;
	private final Map<String, List<Vote>> pending = new ConcurrentHashMap<>();
	private boolean registered = false;

	public PendingVoteManager(VotifierPlus plugin, int delayTicks) {
		this.plugin = plugin;
		this.delayTicks = delayTicks;
		this.file = new File(plugin.getDataFolder(), FILE_NAME);
		load();
		syncListener();
	}

	private void load() {
		if (!file.exists() || file.length() == 0) return;
		try (Reader r = new FileReader(file)) {
			Map<String, List<Vote>> data = GSON.fromJson(r, TYPE);
			if (data != null) pending.putAll(data);
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to load pending votes: " + e.getMessage());
		}
	}

	private void save() {
		try {
			file.getParentFile().mkdirs();
			try (Writer w = new FileWriter(file)) {
				GSON.toJson(pending, TYPE, w);
			}
		} catch (Exception e) {
			plugin.getLogger().warning("Failed to save pending votes: " + e.getMessage());
		}
	}

	private void syncListener() {
		if (!pending.isEmpty()) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
			registered = true;
		}
	}

	private void checkRegister() {
		boolean hasData = !pending.isEmpty();
		if (hasData && !registered) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
			registered = true;
		} else if (!hasData && registered) {
			HandlerList.unregisterAll(this);
			registered = false;
		}
	}

	/**
	 * Queue a vote for a player who is currently offline.
	 * The vote will be delivered via VotifierEvent when the player joins.
	 */
	public void queueVote(Vote vote) {
		String key = vote.getUsername().toLowerCase(Locale.ROOT);
		pending.computeIfAbsent(key, k -> new ArrayList<>()).add(vote);
		plugin.getLogger().info("Pending vote queued for " + vote.getUsername() + " (" + pending.get(key).size() + " pending).");
		save();
		checkRegister();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		String key = event.getPlayer().getName().toLowerCase(Locale.ROOT);
		List<Vote> votes = pending.remove(key);
		if (votes == null || votes.isEmpty()) return;

		plugin.getLogger().info("Player " + event.getPlayer().getName() + " joined, delivering " + votes.size() + " pending vote(s) in " + delayTicks + " tick(s).");
		Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> {
			for (Vote vote : votes) {
				plugin.getLogger().info("Firing pending vote event for " + vote.getUsername() + " from " + vote.getServiceName() + ".");
				Bukkit.getPluginManager().callEvent(
					new com.vexsoftware.votifier.model.VotifierEvent(vote));
			}
		}, delayTicks);
		save();
		checkRegister();
	}
}
