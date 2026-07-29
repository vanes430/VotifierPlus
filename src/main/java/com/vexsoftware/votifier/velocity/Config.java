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
package com.vexsoftware.votifier.velocity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public class Config {

	private ConfigurationNode root;
	private File file;
	private Logger logger;
	private final List<String> errors = new ArrayList<>();

	public Config(File file, Logger logger) {
		this.file = file;
		this.logger = logger;
		reload();
	}

	public void reload() {
		try {
			YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(file.toPath()).build();
			root = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void save() {
		try {
			YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(file.toPath()).build();
			loader.save(root);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ConfigurationNode getNode(Object... path) {
		return root.node(path);
	}

	public String getHost() {
		String h = root.node("host").getString("");
		if (h == null || h.isEmpty()) {
			errors.add("config.yml 'host' is empty or missing");
		}
		return h;
	}

	public int getPort() {
		int p = root.node("port").getInt(0);
		if (p < 1 || p > 65535) {
			errors.add("config.yml 'port' " + p + " is invalid (must be 1-65535)");
		}
		return p;
	}

	public boolean isValid() {
		return errors.isEmpty();
	}

	public List<String> getErrors() {
		return errors;
	}

	public boolean getDebug() {
		String level = root.node("DebugLevel").getString("NONE");
		return !"NONE".equalsIgnoreCase(level);
	}

	public @NonNull Collection<? extends ConfigurationNode> getServers() {
		return root.node("Forwarding").childrenMap().values();
	}

	public ConfigurationNode getServersData(String s) {
		return root.node("Forwarding", s);
	}

	public @NonNull Collection<? extends ConfigurationNode> getTokens() {
		return root.node("tokens").childrenMap().values();
	}

	public String getToken(String key) {
		return root.node("tokens", key).getString("");
	}

	public boolean containsTokens() {
		return !root.node("tokens").virtual();
	}

	public void setToken(String key, String token) {
		try {
			root.node("tokens", key).set(token);
		} catch (SerializationException e) {
			e.printStackTrace();
		}
		save();
	}

	public boolean getTokenSupport() {
		return root.node("TokenSupport").getBoolean(false);
	}
}
