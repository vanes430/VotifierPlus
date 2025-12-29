package com.vexsoftware.votifier;

import com.vexsoftware.votifier.commands.BungeeReloadCommand;
import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.crypto.TokenUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VoteReceiver;
import net.md_5.bungee.api.plugin.Plugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VotifierPlusBungee extends Plugin {

    private Map<String, Object> config;
    private VoteReceiver voteReceiver;
    private KeyPair keyPair;

    @Override
    public void onEnable() {
        getLogger().info("Initializing VotifierPlus for BungeeCord...");
        loadConfig();
        loadKeys();
        startVoteReceiver();
        
        getProxy().getPluginManager().registerCommand(this, new BungeeReloadCommand(this));
    }

    @Override
    public void onDisable() {
        if (voteReceiver != null) {
            voteReceiver.shutdown();
        }
    }

    public void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }
        try (InputStream in = new FileInputStream(configFile)) {
            config = new Yaml().load(in);
        } catch (Exception e) {
            getLogger().severe("Could not load config.yml: " + e.getMessage());
        }
    }

    private void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("host: 0.0.0.0\n");
            writer.write("port: 8192\n");
            writer.write("debug: false\n");
            writer.write("forwarding:\n");
            writer.write("  server1:\n");
            writer.write("    address: 127.0.0.1:8193\n");
            writer.write("    token: ''\n");
            writer.write("    usetoken: false\n");
            writer.write("    enabled: false\n");
        } catch (IOException e) {
            getLogger().severe("Could not create default config: " + e.getMessage());
        }
    }

    private void loadKeys() {
        File rsaDir = new File(getDataFolder(), "rsa");
        try {
            if (!rsaDir.exists()) {
                rsaDir.mkdir();
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDir, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDir);
            }
        } catch (Exception e) {
            getLogger().severe("Error loading RSA keys: " + e.getMessage());
        }
    }

    private void startVoteReceiver() {
        if (voteReceiver != null) {
            voteReceiver.shutdown();
        }
        
        String host = (String) config.getOrDefault("host", "0.0.0.0");
        int port = (int) config.getOrDefault("port", 8192);

        try {
            voteReceiver = new VoteReceiver(host, port) {
                @Override public void logWarning(String warn) { getLogger().warning(warn); }
                @Override public void logSevere(String msg) { getLogger().severe(msg); }
                @Override public void log(String msg) { getLogger().info(msg); }
                @Override public boolean isUseTokens() { return false; }
                @Override public String getVersion() { return getDescription().getVersion() + "-Bungee"; }
                @Override public Map<String, Key> getTokens() { return new HashMap<>(); }
                @Override public Set<String> getServers() {
                    Map<String, Object> forwarding = (Map<String, Object>) config.get("forwarding");
                    return forwarding != null ? forwarding.keySet() : Set.of();
                }
                @Override public ForwardServer getServerData(String s) {
                    Map<String, Object> forwarding = (Map<String, Object>) config.get("forwarding");
                    Map<String, Object> sc = (Map<String, Object>) forwarding.get(s);
                    String addr = (String) sc.get("address");
                    String[] split = addr.split(":");
                    String tokenStr = (String) sc.getOrDefault("token", "");
                    Key tKey = (tokenStr != null && !tokenStr.isEmpty()) ? TokenUtil.createKeyFrom(tokenStr) : null;
                    return new ForwardServer((Boolean) sc.getOrDefault("enabled", true), split[0], Integer.parseInt(split[1]), (String) sc.getOrDefault("key", ""), tKey, (Boolean) sc.getOrDefault("usetoken", false));
                }
                @Override public KeyPair getKeyPair() { return keyPair; }
                @Override public void debug(String msg) { if (Boolean.TRUE.equals(config.get("debug"))) getLogger().info("[DEBUG] " + msg); }
                @Override public void debug(Exception e) { if (Boolean.TRUE.equals(config.get("debug"))) e.printStackTrace(); }
                @Override public void callEvent(Vote e) { getLogger().info("Vote received for " + e.getUsername()); }
            };
            voteReceiver.start();
            getLogger().info("VotifierPlus started on " + host + ":" + port);
        } catch (Exception e) {
            getLogger().severe("Failed to start VoteReceiver: " + e.getMessage());
        }
    }

    public void reload() {
        int oldPort = (int) config.getOrDefault("port", 8192);
        loadConfig();
        loadKeys();
        startVoteReceiver();
        
        int newPort = (int) config.getOrDefault("port", 8192);
        if (oldPort != newPort) {
            getLogger().warning("Port changed during reload. A BungeeCord restart is required for the new port to take effect!");
        }
    }
}
