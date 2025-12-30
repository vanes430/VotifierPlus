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

@Plugin(id = "votifierplus", name = "VotifierPlus", version = "2.0.0-SNAPSHOT",
        description = "A Votifier proxy listener for Velocity", authors = {"vanes430"})
public class VotifierPlusVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private Map<String, Object> config;
    private VoteReceiver voteReceiver;
    private KeyPair keyPair;
    private Map<String, Key> tokens = new HashMap<>();

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
        loadTokens();
        checkAndGenerateKeys();
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
        File folder = dataDirectory.toFile();
        File configFile = new File(folder, "config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            new Yaml().dump(config, writer);
        } catch (IOException e) {
            logger.error("Could not save config.yml", e);
        }
    }

    private void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("# +-------------------------------------------------------------------------+\n");
            writer.write("# |                   VotifierPlus Velocity Configuration                   |\n");
            writer.write("# +-------------------------------------------------------------------------+\n\n");
            writer.write("# The IP address to listen on. 0.0.0.0 listens on all interfaces.\n");
            writer.write("Host: 0.0.0.0\n\n");
            writer.write("# The port to listen on. Default is 8192.\n");
            writer.write("Port: 8192\n\n");
            writer.write("# Enable debug logging for troubleshooting.\n");
            writer.write("Debug: false\n\n");
            writer.write("# Experimental: Enable V2 Token support (NuVotifier compatible).\n");
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
            logger.error("Could not create default config", e);
        }
    }

    private void checkAndGenerateKeys() {
        File rsaDir = new File(dataDirectory.toFile(), "rsa");
        try {
            if (!rsaDir.exists()) {
                rsaDir.mkdir();
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDir, keyPair);
                logger.info("RSA keys generated.");
            } else {
                File publicFile = new File(rsaDir, "public.key");
                File privateFile = new File(rsaDir, "private.key");
                if (!publicFile.exists() || !privateFile.exists()) {
                    keyPair = RSAKeygen.generate(2048);
                    RSAIO.save(rsaDir, keyPair);
                    logger.info("RSA keys missing, regenerated.");
                } else {
                    keyPair = RSAIO.load(rsaDir);
                }
            }
        } catch (Exception e) {
            logger.error("Error reading/generating RSA keys", e);
        }
    }

    private void startVoteReceiver() {
        if (voteReceiver != null) {
            voteReceiver.shutdown();
            voteReceiver = null;
        }
        
        String host = (String) config.getOrDefault("Host", "0.0.0.0");
        int port = (int) config.getOrDefault("Port", 8192);

        try {
            voteReceiver = new VoteReceiver(host, port) {
                @Override public void logWarning(String warn) { logger.warn(warn); }
                @Override public void logSevere(String msg) { logger.error(msg); }
                @Override public void log(String msg) { logger.info(msg); }
                @Override public boolean isUseTokens() { return (Boolean) config.getOrDefault("TokenSupport", false); }
                @Override public String getVersion() { return "1.4.3-Velocity"; }
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
                @Override public void debug(String msg) { if (Boolean.TRUE.equals(config.get("Debug"))) logger.info("[DEBUG] " + msg); }
                @Override public void debug(Exception e) { if (Boolean.TRUE.equals(config.get("Debug"))) logger.error("Debug Error", e); }
                @Override public void callEvent(Vote e) { logger.info("Vote received for " + e.getUsername()); }
            };
            voteReceiver.start();
            logger.info("VotifierPlus listening on " + host + ":" + port);
        } catch (Exception e) {
            logger.error("Failed to start VoteReceiver", e);
        }
    }

    public void reload() {
        loadConfig();
        loadTokens();
        checkAndGenerateKeys();
        startVoteReceiver();
        logger.info("VotifierPlus reloaded. Listening on port " + config.getOrDefault("Port", 8192));
    }
}
