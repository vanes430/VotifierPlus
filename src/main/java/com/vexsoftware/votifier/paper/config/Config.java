/*
 * Copyright (C) 2012 Vex Software LLC
 * Based on VotifierPlus by BenCodez (https://github.com/BenCodez/VotifierPlus).
 * Optimizations by vanes430.
 * This file is part of VotifierPlus.
 *
 * VotifierPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VotifierPlus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VotifierPlus.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.vexsoftware.votifier.paper.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.vexsoftware.votifier.paper.VotifierPlus;

public class Config {

	private VotifierPlus plugin;
	private File configFile;

	private FileConfiguration data;

	private boolean justCreated = false;

	public enum DebugLevel {
		NONE, INFO, EXTRA, DEV;

		public static DebugLevel getDebug(String name) {
			for (DebugLevel d : values()) {
				if (d.name().equalsIgnoreCase(name))
					return d;
			}
			return NONE;
		}

		public boolean isDebug(DebugLevel level) {
			return this.ordinal() >= level.ordinal();
		}

		public boolean isDebug() {
			return this != NONE;
		}
	}

	private String host;
	private int port;
	private DebugLevel debug;
	private Set<String> servers = new HashSet<String>();
	private String formatNoPerms = "&cYou do not have enough permission!";
	private String helpLine = "&3&l%Command% - &3%HelpMessage%";
	private boolean tokenSupport = false;
	private int pendingVoteDelay = 0;
	private final List<String> errors = new ArrayList<>();

	public Config(VotifierPlus plugin) {
		this.plugin = plugin;
		this.configFile = new File(plugin.getDataFolder(), "config.yml");
	}

	public void setup() {
		if (!configFile.exists()) {
			plugin.saveResource("config.yml", false);
			justCreated = true;
		}
		data = YamlConfiguration.loadConfiguration(configFile);
	}

	public boolean isJustCreated() {
		return justCreated;
	}

	public FileConfiguration getData() {
		return data;
	}

	public void loadValues() {
		errors.clear();
		host = data.getString("host", "0.0.0.0");
		if (host == null || host.isEmpty()) {
			errors.add("config.yml 'host' is empty or missing");
		}
		port = data.getInt("port", 8192);
		if (port < 1 || port > 65535) {
			errors.add("config.yml 'port' " + port + " is invalid (must be 1-65535)");
		}
		String debugLevelStr = data.getString("DebugLevel", "NONE");
		debug = DebugLevel.getDebug(debugLevelStr);

		ConfigurationSection forwardingSection = data.getConfigurationSection("Forwarding");
		if (forwardingSection != null) {
			servers = forwardingSection.getKeys(false);
		} else {
			servers = new HashSet<String>();
		}

		formatNoPerms = data.getString("Format.NoPerms", "&cYou do not have enough permission!");
		helpLine = data.getString("Format.HelpLine", "&3&l%Command% - &3%HelpMessage%");
		tokenSupport = data.getBoolean("TokenSupport", false);
		pendingVoteDelay = Math.max(20, data.getInt("PendingVoteDelay", 20));
	}

	public void saveData() {
		try {
			data.save(configFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void reloadData() {
		data = YamlConfiguration.loadConfiguration(configFile);
		loadValues();
	}

	public void setValue(String path, Object value) {
		data.set(path, value);
		saveData();
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public DebugLevel getDebug() {
		return debug;
	}

	public Set<String> getServers() {
		return servers;
	}

	public String getFormatNoPerms() {
		return formatNoPerms;
	}

	public String getHelpLine() {
		return helpLine;
	}

	public boolean isTokenSupport() {
		return tokenSupport;
	}

	public int getPendingVoteDelay() {
		return pendingVoteDelay;
	}

	public ConfigurationSection getForwardingConfiguration(String s) {
		ConfigurationSection forwardingSection = data.getConfigurationSection("Forwarding");
		if (forwardingSection != null) {
			return forwardingSection.getConfigurationSection(s);
		}
		return null;
	}

	public boolean isValid() {
		return errors.isEmpty();
	}

	public List<String> getErrors() {
		return errors;
	}
}
