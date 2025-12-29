package com.vexsoftware.votifier;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.vexsoftware.votifier.commands.VelocityReloadCommand;
import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.crypto.TokenUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VoteReceiver;
import lombok.Getter;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Plugin(id = "votifierplus", name = "VotifierPlus", version = "1.4.3",
        description = "A Votifier proxy listener for Velocity", authors = {"BenCodez"})
public class VotifierPlusVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private Map<String, Object> config;
    private VoteReceiver voteReceiver;
    private KeyPair keyPair;

    @Inject
    public VotifierPlusVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initializing VotifierPlus for Velocity...");
        loadConfig();
        loadKeys();
        startVoteReceiver();
        
        server.getCommandManager().register("vevotifierplus", new VelocityReloadCommand(this));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (voteReceiver != null) {
            voteReceiver.shutdown();
        }
    }

    public void loadConfig() {
        File folder = dataDirectory.toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File configFile = new File(folder, "config.yml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }
        try (InputStream in = new FileInputStream(configFile)) {
            config = new Yaml().load(in);
        } catch (Exception e) {
            logger.error("Could not load config.yml", e);
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
            logger.error("Could not create default config", e);
        }
    }

    private void loadKeys() {
        File rsaDir = new File(dataDirectory.toFile(), "rsa");
        try {
            if (!rsaDir.exists()) {
                rsaDir.mkdir();
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDir, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDir);
            }
        } catch (Exception e) {
            logger.error("Error loading RSA keys", e);
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
                @Override public void logWarning(String warn) { logger.warn(warn); }
                @Override public void logSevere(String msg) { logger.error(msg); }
                @Override public void log(String msg) { logger.info(msg); }
                @Override public boolean isUseTokens() { return false; }
                @Override public String getVersion() { return "1.4.3-Velocity"; }
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
                @Override public void debug(String msg) { if (Boolean.TRUE.equals(config.get("debug"))) logger.info("[DEBUG] " + msg); }
                @Override public void debug(Exception e) { if (Boolean.TRUE.equals(config.get("debug"))) logger.error("Debug Error", e); }
                @Override public void callEvent(Vote e) { logger.info("Vote received for " + e.getUsername()); }
            };
            voteReceiver.start();
            logger.info("VotifierPlus started on " + host + ":" + port);
        } catch (Exception e) {
            logger.error("Failed to start VoteReceiver", e);
        }
    }

    public void reload() {
        int oldPort = (int) config.getOrDefault("port", 8192);
        loadConfig();
        loadKeys();
        startVoteReceiver();
        
        int newPort = (int) config.getOrDefault("port", 8192);
        if (oldPort != newPort) {
            logger.warn("Port changed during reload. A proxy restart is required for the new port to take effect!");
        }
    }
}
