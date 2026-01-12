/*
 * Copyright (C) 2012 Vex Software LLC
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

package com.vexsoftware.votifier;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.vexsoftware.votifier.commands.CommandVotifierPlus;
import com.vexsoftware.votifier.config.Config;
import com.vexsoftware.votifier.util.AsciiArt;
import com.vexsoftware.votifier.util.Debug;
import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.crypto.TokenUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import com.vexsoftware.votifier.net.VoteReceiver;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;

import lombok.Getter;
import lombok.Setter;

/**
 * The main Votifier plugin class.
 * 
 * @author Blake Beaupain
 * @author Kramer Campbell
 */
public class VotifierPlus extends JavaPlugin implements Listener {

	private HashMap<String, ArrayList<Vote>> waitingList = new HashMap<String, ArrayList<Vote>>();

	public HashMap<String, ArrayList<Vote>> getWaitingList() {
		return waitingList;
	}

	/** The Votifier instance. */
	private static VotifierPlus instance;

	@Getter
	private FoliaLib foliaLib;

	@Getter
	public Config configFile;

	/** The vote receiver. */
	private VoteReceiver voteReceiver;

	/** The RSA key pair. */
	@Setter
	private KeyPair keyPair;

	private HashMap<String, Key> tokens = new HashMap<String, Key>();

	private void loadTokens() {
		tokens.clear();
		FileConfiguration config = getConfig();
		if (!config.contains("Tokens")) {
			configFile.setValue("Tokens.default", TokenUtil.newToken());
		}

		ConfigurationSection section = config.getConfigurationSection("Tokens");
		if (section != null) {
			for (String key : section.getKeys(false)) {
				tokens.put(key, TokenUtil.createKeyFrom(section.getString(key)));
			}
		}
	}

	private void checkAndGenerateKeys() {
		File rsaDirectory = new File(getDataFolder() + "/rsa");
		try {
			if (!rsaDirectory.exists()) {
				rsaDirectory.mkdir();
				keyPair = RSAKeygen.generate(2048);
				RSAIO.save(rsaDirectory, keyPair);
				getLogger().info("RSA keys generated.");
			} else {
				File publicFile = new File(rsaDirectory, "public.key");
				File privateFile = new File(rsaDirectory, "private.key");
				if (!publicFile.exists() || !privateFile.exists()) {
					keyPair = RSAKeygen.generate(2048);
					RSAIO.save(rsaDirectory, keyPair);
					getLogger().info("RSA keys missing, regenerated.");
				} else {
					keyPair = RSAIO.load(rsaDirectory);
				}
			}
		} catch (Exception ex) {
			getLogger().severe("Error reading/generating RSA keys: " + ex.getMessage());
			gracefulExit();
		}
	}

	@Override
	public void onEnable() {
		AsciiArt.send(getLogger()::info);
		instance = this;
		foliaLib = new FoliaLib(this);

		configFile = new Config(this);
		// Config setup handled in Config constructor

		// Initial setup for port if config is fresh (simulated)
		if (getConfig().getInt("Port", 0) == 0) {
			int openPort = 8192;
			try {
				ServerSocket s = new ServerSocket();
				s.bind(new InetSocketAddress("0.0.0.0", 0));
				openPort = s.getLocalPort();
				s.close();
			} catch (Exception e) {
			}
			getLogger().info("Configuring Votifier for the first time...");
			configFile.setValue("Port", openPort);
			configFile.setValue("Tokens.default", TokenUtil.newToken());
			getLogger().info("Assigned port: " + openPort);
		}
		
		        configFile.loadValues();
		        loadTokens();
		
		        Debug.initialize(getLogger()::info, getDataFolder(), configFile.isDebug());
		
		        getServer().getPluginManager().registerEvents(this, this);
		getCommand("votifierplus").setExecutor(new CommandVotifierPlus(this));

		checkAndGenerateKeys();

		loadVoteReceiver();
		startAutoClearTask();
	}

	private void loadVoteReceiver() {
		try {
			voteReceiver = new VoteReceiver(configFile.getHost(), configFile.getPort()) {

				@Override
				public void logWarning(String warn) {
					getLogger().warning(warn);
				}

				@Override
				public void logSevere(String msg) {
					getLogger().severe(msg);
				}

				@Override
				public void log(String msg) {
					getLogger().info(msg);
				}

				@Override
				public String getVersion() {
					return getDescription().getVersion();
				}

				@Override
				public Set<String> getServers() {
					return configFile.getServers();
				}

				@Override
				public ForwardServer getServerData(String s) {
					ConfigurationSection d = configFile.getForwardingConfiguration(s);
					if (d == null) return null;
					String token = d.getString("Token", "");
					Key tokenKey = null;
					if (!token.isEmpty()) {
						tokenKey = TokenUtil.createKeyFrom(token);
					}
					return new ForwardServer(d.getBoolean("Enabled"), d.getString("Host", ""), d.getInt("Port"),
							d.getString("Key", ""), tokenKey, d.getBoolean("UseToken", false));
				}

				@Override
				public KeyPair getKeyPair() {
					return instance.getKeyPair();
				}

				@Override
				public void debug(Exception e) {
					instance.debug(e);
				}

				@Override
				public void debug(String debug) {
					instance.debug(debug);
				}

				@Override
				public void callEvent(Vote vote) {
					foliaLib.getImpl().runNextTick(task -> {
						Player player = Bukkit.getPlayer(vote.getUsername());
						if (player != null) {
							Bukkit.getServer().getPluginManager().callEvent(new VotifierEvent(vote));
						} else {
							if (!waitingList.containsKey(vote.getUsername())) {
								waitingList.put(vote.getUsername(), new ArrayList<Vote>());
							}
							waitingList.get(vote.getUsername()).add(vote);
							debug("Player " + vote.getUsername() + " is not online, adding to waiting list");
						}
					});
				}

				@Override
				public Map<String, Key> getTokens() {
					return tokens;
				}

				@Override
				public boolean isUseTokens() {
					return configFile.isTokenSupport();
				}

				@Override
				public boolean isLogFailedVotes() {
					return configFile.isLogFailedVotes();
				}
			};
			voteReceiver.start();

			getLogger().info("Votifier enabled.");
		} catch (Exception ex) {
			gracefulExit();
			return;
		}
	}

	public void debug(String debug) {
		if (configFile.isDebug()) {
			getLogger().info("[Debug] " + debug);
		}
	}

	public void debug(Exception e) {
		if (configFile.isDebug()) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		AsciiArt.send(getLogger()::info);
		// Interrupt the vote receiver.
		if (voteReceiver != null) {
			voteReceiver.shutdown();
		}
		getLogger().info("Votifier disabled.");
	}

	private void gracefulExit() {
		getLogger().severe("Votifier did not initialize properly!");
	}

	/**
	 * Gets the instance.
	 * 
	 * @return The instance
	 */
	public static VotifierPlus getInstance() {
		return instance;
	}

	/**
	 * Gets the vote receiver.
	 * 
	 * @return The vote receiver
	 */
	public VoteReceiver getVoteReceiver() {
		return voteReceiver;
	}

	/**
	 * Gets the keyPair.
	 * 
	 * @return The keyPair
	 */
	public KeyPair getKeyPair() {
		return keyPair;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		final String name = event.getPlayer().getName();
		if (waitingList.containsKey(name)) {
			int delay = configFile.getWaitingDelay();
			if (delay <= 0) {
				processWaitingVotes(name);
			} else {
				foliaLib.getImpl().runLater(() -> processWaitingVotes(name), delay, TimeUnit.SECONDS);
			}
		}
	}

	private void processWaitingVotes(String name) {
		if (waitingList.containsKey(name)) {
			ArrayList<Vote> votes = waitingList.get(name);
			for (Vote vote : votes) {
				Bukkit.getServer().getPluginManager().callEvent(new VotifierEvent(vote));
			}
			waitingList.remove(name);
			debug("Processed waiting votes for " + name);
		}
	}

	public void reload() {
		if (voteReceiver != null) {
			voteReceiver.shutdown();
			voteReceiver = null;
		}
		
		configFile.reloadData();
		checkAndGenerateKeys();
		loadTokens();
		loadVoteReceiver();
		startAutoClearTask();
		
		getLogger().info("VotifierPlus reloaded. Listening on port " + configFile.getPort());
	}

	public void startAutoClearTask() {
		long delay = configFile.getAutoClearDelay();
		if (delay > 0) {
			foliaLib.getImpl().runTimerAsync(task -> {
				if (!waitingList.isEmpty()) {
					waitingList.clear();
					debug("Cleared waiting list (Auto-Clear)");
				}
			}, delay, delay, TimeUnit.SECONDS);
		}
	}
}