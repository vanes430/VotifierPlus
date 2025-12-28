package com.vexsoftware.votifier.velocity;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public class Config {
	private File file;
	private ConfigurationNode node;
	private YamlConfigurationLoader loader;

	public Config(File file) {
		this.file = file;
		this.loader = YamlConfigurationLoader.builder().file(file).build();
		load();
	}

	public void load() {
		try {
			node = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void save() {
		try {
			loader.save(node);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reload() {
		load();
	}

	public String getHost() {
		return node.node("host").getString("");
	}

	public int getPort() {
		return node.node("port").getInt(0);
	}

	public boolean getDebug() {
		return node.node("Debug").getBoolean(false);
	}

	public @NonNull Collection<? extends ConfigurationNode> getServers() {
		return node.node("Forwarding").childrenMap().values();
	}

	public ConfigurationNode getServersData(String s) {
		return node.node("Forwarding", s);
	}

	public @NonNull Collection<? extends ConfigurationNode> getTokens() {
		return node.node("tokens").childrenMap().values();
	}

	public String getToken(String key) {
		return node.node("tokens", key).getString(null);
	}

	public boolean containsTokens() {
		return node.node("tokens").rawScalar() != null || !node.node("tokens").childrenMap().isEmpty();
	}

	public void setToken(String key, String token) {
		try {
			node.node("tokens", key).set(token);
		} catch (Exception e) {
			e.printStackTrace();
		}
		save();
	}

	public boolean getTokenSupport() {
		return node.node("TokenSupport").getBoolean(false);
	}
}