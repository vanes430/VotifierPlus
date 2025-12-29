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
    private Map<String, Key> tokens = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("Initializing VotifierPlus for BungeeCord...");
        loadConfig();
        loadTokens();
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

    private void loadTokens() {
        tokens.clear();
        Map<String, String> tokensConfig = (Map<String, String>) config.get("Tokens");
        if (tokensConfig == null) {
            tokensConfig = new HashMap<>();
            tokensConfig.put("default", TokenUtil.newToken());
            config.put("Tokens", tokensConfig);
            saveConfig();
        }
        for (Map.Entry<String, String> entry : tokensConfig.entrySet()) {
            tokens.put(entry.getKey(), TokenUtil.createKeyFrom(entry.getValue()));
        }
    }

    private void saveConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            new Yaml().dump(config, writer);
        } catch (IOException e) {
            getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    private void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("# +-------------------------------------------------------------------------+\n");
            writer.write("# |                    VotifierPlus Bungee Configuration                    |\n");
            writer.write("# +-------------------------------------------------------------------------+\n\n");
            writer.write("# The IP address to listen on. 0.0.0.0 listens on all interfaces.\n");
            writer.write("Host: 0.0.0.0\n\n");
            writer.write("# The port to listen on. Default is 8192.\n");
            writer.write("Port: 8192\n\n");
            writer.write("# Enable debug logging for troubleshooting.\n");
            writer.write("Debug: false\n\n");
            writer.write("# Experimental: Enable V2 Token support.\n");
            writer.write("TokenSupport: false\n\n");
            writer.write("# Tokens for V2 authentication.\n");
            writer.write("Tokens:\n");
            writer.write("  default: '" + TokenUtil.newToken() + "'\n\n");
            writer.write("# Vote Forwarding: Send votes received by the proxy to your game servers.\n");
            writer.write("Forwarding:\n");
            writer.write("  server1:\n");
            writer.write("    # Address of the target server (host:port).\n");
            writer.write("    Address: 127.0.0.1:8193\n");
            writer.write("    # RSA Public Key of the target server.\n");
            writer.write("    Key: ''\n");
            writer.write("    # Token for V2 authentication (if UseToken is true).\n");
            writer.write("    Token: ''\n");
            writer.write("    # Use V2 Token authentication instead of RSA keys.\n");
            writer.write("    UseToken: false\n");
            writer.write("    # Whether forwarding to this server is enabled.\n");
            writer.write("    Enabled: false\n");
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
        
        String host = (String) config.getOrDefault("Host", "0.0.0.0");
        int port = (int) config.getOrDefault("Port", 8192);

        try {
            voteReceiver = new VoteReceiver(host, port) {
                @Override public void logWarning(String warn) { getLogger().warning(warn); }
                @Override public void logSevere(String msg) { getLogger().severe(msg); }
                @Override public void log(String msg) { getLogger().info(msg); }
                @Override public boolean isUseTokens() { return (Boolean) config.getOrDefault("TokenSupport", false); }
                @Override public String getVersion() { return getDescription().getVersion() + "-Bungee"; }
                @Override public Map<String, Key> getTokens() { return tokens; }
                @Override public Set<String> getServers() {
                    Map<String, Object> forwarding = (Map<String, Object>) config.get("Forwarding");
                    return forwarding != null ? forwarding.keySet() : Set.of();
                }
                @Override public ForwardServer getServerData(String s) {
                    Map<String, Object> forwarding = (Map<String, Object>) config.get("Forwarding");
                    Map<String, Object> sc = (Map<String, Object>) forwarding.get(s);
                    String addr = (String) sc.getOrDefault("Address", "127.0.0.1:8192");
                    String[] split = addr.split(":");
                    String tokenStr = (String) sc.getOrDefault("Token", "");
                    Key tKey = (tokenStr != null && !tokenStr.isEmpty()) ? TokenUtil.createKeyFrom(tokenStr) : null;
                    return new ForwardServer((Boolean) sc.getOrDefault("Enabled", true), split[0], Integer.parseInt(split[1]), (String) sc.getOrDefault("Key", ""), tKey, (Boolean) sc.getOrDefault("UseToken", false));
                }
                @Override public KeyPair getKeyPair() { return keyPair; }
                @Override public void debug(String msg) { if (Boolean.TRUE.equals(config.get("Debug"))) getLogger().info("[DEBUG] " + msg); }
                @Override public void debug(Exception e) { if (Boolean.TRUE.equals(config.get("Debug"))) e.printStackTrace(); }
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
        loadTokens();
        loadKeys();
        startVoteReceiver();
        
        int newPort = (int) config.getOrDefault("port", 8192);
        if (oldPort != newPort) {
            getLogger().warning("Port changed during reload. A BungeeCord restart is required for the new port to take effect!");
        }
    }
}
