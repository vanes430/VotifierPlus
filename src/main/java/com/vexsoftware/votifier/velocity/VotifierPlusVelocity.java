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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.vexsoftware.votifier.common.ForwardServer;
import com.vexsoftware.votifier.common.crypto.RSAIO;
import com.vexsoftware.votifier.common.crypto.RSAKeygen;
import com.vexsoftware.votifier.common.crypto.TokenUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.common.net.SharedVoteReceiver;
import com.vexsoftware.votifier.common.net.ThrottleConfig;
import com.vexsoftware.votifier.common.net.VotePlatform;
import com.vexsoftware.votifier.common.net.VoteReceiver;

@Plugin(id = "votifierplus")
public class VotifierPlusVelocity {

	private VoteReceiver voteReceiver;
	private Config config;
	private KeyPair keyPair;
	private ProxyServer server;
	private Logger logger;
	private Path dataDirectory;

	@Inject
	public VotifierPlusVelocity(ProxyServer server, Logger logger,
			@DataDirectory Path dataDirectory) {
		this.server = server;
		this.logger = logger;
		this.dataDirectory = dataDirectory;
	}

	public VoteReceiver getVoteReceiver() { return voteReceiver; }
	public Config getConfig() { return config; }
	public KeyPair getKeyPair() { return keyPair; }
	public void setKeyPair(KeyPair k) { keyPair = k; }
	public Path getDataDirectory() { return dataDirectory; }

	@Subscribe
	public void onProxyDisable(ProxyShutdownEvent event) {
		if (voteReceiver != null) { voteReceiver.shutdown(); voteReceiver = null; }
	}

	private HashMap<String, Key> tokens = new HashMap<String, Key>();

	private void loadTokens() {
		tokens.clear();
		if (!config.containsTokens()) config.setToken("default", TokenUtil.newToken());
		for (ConfigurationNode key : config.getTokens()) {
			String id = String.valueOf(key.key());
			String token = config.getToken(id);
			if (token == null || token.trim().isEmpty()) {
				logger.warn("Skipping empty token for id: " + id);
				continue;
			}
			tokens.put(id, TokenUtil.createKeyFrom(token));
		}
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
		File configFile = new File(dataDirectory.toFile(), "config.yml");
		configFile.getParentFile().mkdirs();
		boolean justCreated = false;
		if (!configFile.exists()) {
			justCreated = true;
			try {
				configFile.createNewFile();
				try (InputStream in = VotifierPlusVelocity.class.getClassLoader()
						.getResourceAsStream("config.yml");
					 FileOutputStream fos = new FileOutputStream(configFile)) {
					byte[] buf = new byte[2048]; int r;
					while (-1 != (r = in.read(buf))) fos.write(buf, 0, r);
				}
			} catch (IOException e) { e.printStackTrace(); }
		}
		config = new Config(configFile, logger);
		loadTokens();
		boolean firstRun = justCreated;

		config.getHost();
		config.getPort();
		if (!config.isValid()) {
			logger.error("============================================================");
			logger.error("  VotifierPlus configuration is invalid!");
			for (String err : config.getErrors()) {
				logger.error("  - {}", err);
			}
			logger.error("  Fix config.yml and run '/votifierplus reload'.");
			logger.error("============================================================");
			return;
		}

		CommandMeta meta = server.getCommandManager().metaBuilder("votifierplus")
				.aliases("vp", "votifierplusproxy").build();
		server.getCommandManager().register(meta, new VotifierPlusVelocityCommand(this));

		File rsaDirectory = new File(dataDirectory.toFile(), "rsa");
		try {
			if (!rsaDirectory.exists()) {
				rsaDirectory.mkdir();
				keyPair = RSAKeygen.generate(2048);
				RSAIO.save(rsaDirectory, keyPair);
			} else {
				keyPair = RSAIO.load(rsaDirectory);
			}
		} catch (Exception ex) {
			logger.error("Error reading configuration file or RSA keys");
			return;
		}
		loadVoteReceiver();
		logger.info("Votifier velocity loaded.");

		if (firstRun) {
			server.getScheduler().buildTask(this, t -> {
				logger.info("============================================================");
				logger.info("  VotifierPlus first-time setup complete!");
				logger.info("  Default port " + config.getPort() + " has been assigned.");
				logger.info("  Edit config.yml to set your preferred port and settings,");
				logger.info("  then run '/votifierplus reload' to apply changes.");
				logger.info("============================================================");
			}).delay(10, TimeUnit.SECONDS).schedule();
		}
	}

	private boolean loadVoteReceiver() {
		try {
			voteReceiver = new SharedVoteReceiver(new VotePlatform() {

				@Override public String getHost() { return config.getHost(); }
				@Override public int getPort() { return config.getPort(); }

				@Override public void logWarning(String msg) { logger.warn(msg); }
				@Override public void logSevere(String msg) { logger.error(msg); }
				@Override public void log(String msg) { logger.info(msg); }

				@Override public boolean isDebug() { return config.getDebug(); }
				@Override public void debugException(Exception e) { e.printStackTrace(); }
				@Override public void debugMessage(String msg) { logger.info("Debug: " + msg); }

				@Override public String getVersion() { return "1.4.4-SNAPSHOT"; }

				@Override
				public Set<String> getServers() {
					Set<String> s = new HashSet<String>();
					for (ConfigurationNode n : config.getServers())
						s.add(String.valueOf(n.key()));
					return s;
				}

				@Override
				public ForwardServer getServerData(String s) {
					ConfigurationNode d = config.getServersData(s);
					String token = d.node("Token").getString("");
					Key k = !token.isEmpty() ? TokenUtil.createKeyFrom(token) : null;
					return new ForwardServer(d.node("Enabled").getBoolean(),
						d.node("Host").getString(), d.node("Port").getInt(),
						d.node("Key").getString(), k);
				}

				@Override public KeyPair getKeyPair() { return keyPair; }

				@Override
				public void callEvent(Vote vote) {
					server.getEventManager()
						.fire(new com.vexsoftware.votifier.velocity.event.VotifierEvent(vote));
				}

				@Override public Map<String, Key> getTokens() { return tokens; }
				@Override public boolean isUseTokens() { return config.getTokenSupport(); }

				@Override
				public ThrottleConfig getThrottleConfig() {
					ConfigurationNode root = config.getNode("ConnectionThrottle");
					if (root == null || root.virtual()) {
						return new ThrottleConfig(false, Collections.<String>emptySet(),
							"2m", 20, "5m", 8, "10m", true, 6, "15m", "60s");
					}
					boolean enabled = root.node("Enabled").getBoolean(true);
					Set<String> tunnelIps = new HashSet<String>();
					ConfigurationNode ipsNode = root.node("TunnelRemoteIps");
					if (!ipsNode.virtual()) {
						for (ConfigurationNode n : ipsNode.childrenList()) {
							Object raw = n.raw();
							if (raw != null) { String s = String.valueOf(raw).trim();
								if (!s.isEmpty()) tunnelIps.add(s); }
						}
					}
					Set<String> finalIps = tunnelIps.isEmpty()
						? Collections.<String>emptySet()
						: Collections.unmodifiableSet(tunnelIps);

					return new ThrottleConfig(enabled, finalIps,
						root.node("Window").getString("2m"),
						root.node("Failures").getInt(20),
						root.node("ThrottleFor").getString("5m"),
						root.node("TunnelFailures").getInt(8),
						root.node("TunnelThrottleFor").getString("10m"),
						root.node("PerClientBan").node("Enabled").getBoolean(true),
						root.node("PerClientBan").node("Failures").getInt(6),
						root.node("PerClientBan").node("BanFor").getString("15m"),
						root.node("LogWindow").getString("60s"));
				}
			});
			voteReceiver.start();
			logger.info("Votifier enabled.");
			return true;
		} catch (Exception ex) {
			logger.error("Unable to start Votifier vote receiver.", ex);
			return false;
		}
	}

	public boolean reload() {
		if (voteReceiver != null) { voteReceiver.shutdown(); voteReceiver = null; }
		config.reload();
		loadTokens();
		return loadVoteReceiver();
	}
}
