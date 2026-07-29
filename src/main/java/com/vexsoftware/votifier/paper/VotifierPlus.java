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
package com.vexsoftware.votifier.paper;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import com.vexsoftware.votifier.paper.config.Config;
import com.vexsoftware.votifier.paper.config.Config.DebugLevel;
import com.vexsoftware.votifier.common.ForwardServer;
import com.vexsoftware.votifier.common.crypto.RSAIO;
import com.vexsoftware.votifier.common.crypto.RSAKeygen;
import com.vexsoftware.votifier.common.crypto.TokenUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import com.vexsoftware.votifier.common.net.SharedVoteReceiver;
import com.vexsoftware.votifier.common.net.ThrottleConfig;
import com.vexsoftware.votifier.common.net.VotePlatform;
import com.vexsoftware.votifier.common.net.VoteReceiver;
import com.vexsoftware.votifier.paper.vote.PendingVoteManager;

public class VotifierPlus extends JavaPlugin {

	private static VotifierPlus instance;

	public Config configFile;

	private VoteReceiver voteReceiver;

	private KeyPair keyPair;

	private PendingVoteManager pendingVoteManager;

	private HashMap<String, Key> tokens = new HashMap<String, Key>();

	public static VotifierPlus getInstance() { return instance; }
	public Config getConfigFile() { return configFile; }
	public VoteReceiver getVoteReceiver() { return voteReceiver; }
	public KeyPair getKeyPair() { return keyPair; }
	public void setKeyPair(KeyPair keyPair) { this.keyPair = keyPair; }

	private void loadTokens() {
		tokens.clear();
		if (!configFile.getData().contains("tokens")) {
			configFile.setValue("tokens.default", TokenUtil.newToken());
		}
		for (String key : configFile.getData().getConfigurationSection("tokens").getKeys(false)) {
			tokens.put(key, TokenUtil.createKeyFrom(configFile.getData().getString("tokens." + key)));
		}
	}

	@Override
	public void onEnable() {
		instance = this;

		configFile = new Config(this);
		configFile.setup();

		if (configFile.isJustCreated()) {
			configFile.getData().set("port", 8192);
			configFile.saveData();
			configFile.setValue("tokens.default", TokenUtil.newToken());

			Bukkit.getGlobalRegionScheduler().runDelayed(this, t -> {
				getLogger().info("============================================================");
				getLogger().info("  VotifierPlus first-time setup complete!");
				getLogger().info("  Default port " + configFile.getData().getInt("port") + " has been assigned.");
				getLogger().info("  Edit config.yml to set your preferred port and settings,");
				getLogger().info("  then run '/votifierplus reload' to apply changes.");
				getLogger().info("============================================================");
			}, 200);
		}
		configFile.loadValues();
		loadTokens();

		if (!configFile.isValid()) {
			getLogger().severe("============================================================");
			getLogger().severe("  VotifierPlus configuration is invalid!");
			for (String err : configFile.getErrors()) {
				getLogger().severe("  - " + err);
			}
			getLogger().severe("  Fix config.yml and run '/votifierplus reload'.");
			getLogger().severe("============================================================");
			gracefulExit();
			return;
		}

		getCommand("votifierplus").setExecutor(new com.vexsoftware.votifier.paper.commands.CommandVotifierPlus(this));
		getCommand("votifierplus").setTabCompleter(new com.vexsoftware.votifier.paper.commands.VotifierPlusTabCompleter());

		File rsaDirectory = new File(getDataFolder() + "/rsa");
		try {
			if (!rsaDirectory.exists()) {
				rsaDirectory.mkdir();
				keyPair = RSAKeygen.generate(2048);
				RSAIO.save(rsaDirectory, keyPair);
			} else {
				keyPair = RSAIO.load(rsaDirectory);
			}
		} catch (Exception ex) {
			getLogger().severe("Error reading configuration file or RSA keys");
			gracefulExit();
			return;
		}

		this.pendingVoteManager = new PendingVoteManager(this, configFile.getPendingVoteDelay());
		loadVoteReceiver();
	}

	private void loadVoteReceiver() {
		try {
			voteReceiver = new SharedVoteReceiver(new VotePlatform() {

				@Override public String getHost() { return configFile.getHost(); }
				@Override public int getPort() { return configFile.getPort(); }

				@Override public void logWarning(String msg) { getLogger().warning(msg); }
				@Override public void logSevere(String msg) { getLogger().severe(msg); }
				@Override public void log(String msg) { getLogger().info(msg); }

				@Override public boolean isDebug() { return configFile.getDebug().isDebug(); }
				@Override public void debugException(Exception e) { e.printStackTrace(); }
				@Override public void debugMessage(String msg) { getLogger().info("Debug: " + msg); }

				@Override public String getVersion() { return getDescription().getVersion(); }

				@Override public Set<String> getServers() { return configFile.getServers(); }

				@Override
				public ForwardServer getServerData(String s) {
					ConfigurationSection d = configFile.getForwardingConfiguration(s);
					String token = d != null ? d.getString("Token", "") : "";
					Key tokenKey = null;
					if (!token.isEmpty()) tokenKey = TokenUtil.createKeyFrom(token);
					return new ForwardServer(
						d != null && d.getBoolean("Enabled"),
						d != null ? d.getString("Host", "") : "",
						d != null ? d.getInt("Port") : 0,
						d != null ? d.getString("Key", "") : "",
						tokenKey
					);
				}

				@Override public KeyPair getKeyPair() { return keyPair; }

				@Override
				public void callEvent(Vote vote) {
					boolean online = Bukkit.getOnlinePlayers().stream()
						.anyMatch(p -> p.getName().equalsIgnoreCase(vote.getUsername()));
					if (online) {
						getLogger().info("Player " + vote.getUsername() + " is online, firing event immediately.");
						Bukkit.getGlobalRegionScheduler().run(instance, task -> {
							Bukkit.getServer().getPluginManager()
								.callEvent(new com.vexsoftware.votifier.model.VotifierEvent(vote));
						});
					} else {
						getLogger().info("Player " + vote.getUsername() + " is offline, queued as pending vote.");
						pendingVoteManager.queueVote(vote);
					}
				}

				@Override public Map<String, Key> getTokens() { return tokens; }
				@Override public boolean isUseTokens() { return configFile.isTokenSupport(); }

				@Override
				public ThrottleConfig getThrottleConfig() {
					ConfigurationSection root = configFile.getData().getConfigurationSection("ConnectionThrottle");
					if (root == null) {
						return new ThrottleConfig(false, Collections.<String>emptySet(),
							"2m", 20, "5m", 8, "10m", true, 6, "15m", "60s");
					}

					boolean enabled = root.getBoolean("Enabled", true);
					java.util.List<String> ips = root.getStringList("TunnelRemoteIps");
					Set<String> tunnelIps = new java.util.HashSet<String>();
					if (ips != null) {
						for (String ip : ips) {
							if (ip != null) { ip = ip.trim(); if (!ip.isEmpty()) tunnelIps.add(ip); }
						}
					}
					Set<String> finalTunnelIps = tunnelIps.isEmpty()
						? Collections.<String>emptySet()
						: Collections.unmodifiableSet(tunnelIps);

					String window = root.getString("Window", "2m");
					int failures = root.getInt("Failures", 20);
					String throttleFor = root.getString("ThrottleFor", "5m");
					int tunnelFailures = root.getInt("TunnelFailures", Math.max(3, failures / 2));
					String tunnelThrottleFor = root.getString("TunnelThrottleFor", "10m");

					ConfigurationSection ban = root.getConfigurationSection("PerClientBan");
					boolean banEnabled = ban == null ? true : ban.getBoolean("Enabled", true);
					int banFailures = ban == null ? 6 : ban.getInt("Failures", 6);
					String banFor = ban == null ? "15m" : ban.getString("BanFor", "15m");
					String logWindow = root.getString("LogWindow", "60s");

					return new ThrottleConfig(enabled, finalTunnelIps, window, failures, throttleFor,
						tunnelFailures, tunnelThrottleFor, banEnabled, banFailures, banFor, logWindow);
				}
			});
			voteReceiver.start();
			getLogger().info("Votifier enabled.");
	} catch (Exception ex) {
			getLogger().severe("Failed to initialize vote receiver: " + ex.getMessage());
			ex.printStackTrace();
			gracefulExit();
		}
	}

	@Override
	public void onDisable() {
		if (voteReceiver != null) voteReceiver.shutdown();
		getLogger().info("Votifier disabled.");
	}

	private void gracefulExit() {
		getLogger().severe("Votifier did not initialize properly!");
	}

	public void reload() {
		if (voteReceiver != null) voteReceiver.shutdown();
		configFile.reloadData();
		loadTokens();
		loadVoteReceiver();
	}
}
