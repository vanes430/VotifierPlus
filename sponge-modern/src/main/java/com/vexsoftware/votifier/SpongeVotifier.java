package com.vexsoftware.votifier;

import com.google.inject.Inject;
import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.crypto.TokenUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VoteReceiver;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.util.*;

@Plugin("votifierplus")
public class SpongeVotifier {

    private final Logger logger;
    private final Path configDir;
    private final ConfigurationLoader<CommentedConfigurationNode> configLoader;

    private CommentedConfigurationNode config;
    private VoteReceiver voteReceiver;
    private KeyPair keyPair;
    private Map<String, Key> tokens = new HashMap<>();

    @Inject
    public SpongeVotifier(Logger logger, @ConfigDir(sharedRoot = false) Path configDir, @DefaultConfig(sharedRoot = false) ConfigurationLoader<CommentedConfigurationNode> configLoader) {
        this.logger = logger;
        this.configDir = configDir;
        this.configLoader = configLoader;
    }

    @Listener
    public void onConstruct(ConstructPluginEvent event) {
        // Plugin construction logic if needed
    }

    @Listener
    public void onServerStarting(StartingEngineEvent<org.spongepowered.api.Server> event) {
        logger.info("Initializing VotifierPlus for Sponge (Modern)...");

        File configDirFile = configDir.toFile();
        if (!configDirFile.exists()) {
            configDirFile.mkdirs();
        }

        loadConfig();
        loadTokens();

        File rsaDirectory = new File(configDirFile, "rsa");
        try {
            if (!rsaDirectory.exists()) {
                rsaDirectory.mkdir();
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDirectory);
            }
        } catch (Exception ex) {
            logger.error("Error reading configuration file or RSA keys", ex);
            return;
        }

        String host = config.node("Host").getString("0.0.0.0");
        int port = config.node("Port").getInt(8192);

        if (port == 0) {
            try {
                ServerSocket s = new ServerSocket();
                s.bind(new InetSocketAddress(host, 0));
                port = s.getLocalPort();
                s.close();
                config.node("Port").set(port);
                saveConfig();
            } catch (Exception e) {
                port = 8192;
            }
        }

        final int finalPort = port;

        try {
            voteReceiver = new VoteReceiver(host, finalPort) {
                @Override
                public void logWarning(String warn) {
                    logger.warn(warn);
                }

                @Override
                public void logSevere(String msg) {
                    logger.error(msg);
                }

                @Override
                public void log(String msg) {
                    logger.info(msg);
                }

                @Override
                public boolean isUseTokens() {
                    return config.node("TokenSupport").getBoolean(false);
                }

                @Override
                public String getVersion() {
                    return "Sponge-Modern-2.0.0-SNAPSHOT";
                }

                @Override
                public Map<String, Key> getTokens() {
                    return tokens;
                }

                @Override
                public Set<String> getServers() {
                    ConfigurationNode forwardingNode = config.node("Forwarding");
                    if (forwardingNode.virtual() || !forwardingNode.isMap()) {
                        return Collections.emptySet();
                    }
                    Set<String> servers = new HashSet<>();
                    for (Object key : forwardingNode.childrenMap().keySet()) {
                        servers.add(key.toString());
                    }
                    return servers;
                }

                @Override
                public ForwardServer getServerData(String s) {
                    ConfigurationNode node = config.node("Forwarding", s);
                    if (node.virtual()) return null;

                    boolean enabled = node.node("Enabled").getBoolean(false);
                    String host = node.node("Host").getString("");
                    int port = node.node("Port").getInt(8192);
                    String key = node.node("Key").getString("");
                    String token = node.node("Token").getString("");
                    boolean useToken = node.node("UseToken").getBoolean(false);

                    Key tokenKey = null;
                    if (!token.isEmpty()) {
                        tokenKey = TokenUtil.createKeyFrom(token);
                    }

                    return new ForwardServer(enabled, host, port, key, tokenKey, useToken);
                }

                @Override
                public KeyPair getKeyPair() {
                    return keyPair;
                }

                @Override
                public void debug(String msg) {
                    if (config.node("Debug").getBoolean(false)) {
                        logger.info("[DEBUG] " + msg);
                    }
                }

                @Override
                public void debug(Exception e) {
                    if (config.node("Debug").getBoolean(false)) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void callEvent(Vote vote) {
                     Sponge.eventManager().post(new VotifierEvent(vote));
                }
            };
            voteReceiver.start();
            logger.info("VotifierPlus loaded on port " + finalPort + ".");
        } catch (Exception e) {
            logger.error("Failed to start VoteReceiver", e);
        }
    }

    @Listener
    public void onServerStopping(StoppingEngineEvent<org.spongepowered.api.Server> event) {
        if (voteReceiver != null) {
            voteReceiver.shutdown();
        }
        logger.info("VotifierPlus disabled.");
    }

    private void loadConfig() {
        try {
            if (!new File(configDir.toFile(), "votifierplus.conf").exists()) {
                config = configLoader.createNode();
                config.node("Host").set("0.0.0.0");
                config.node("Port").set(8192);
                config.node("Debug").set(false);
                config.node("TokenSupport").set(false);
                config.node("Tokens", "default").set(TokenUtil.newToken());
                
                ConfigurationNode forwarding = config.node("Forwarding", "example");
                forwarding.node("Enabled").set(false);
                forwarding.node("Host").set("127.0.0.1");
                forwarding.node("Port").set(8193);
                forwarding.node("Key").set("");
                forwarding.node("Token").set("");
                forwarding.node("UseToken").set(false);
                
                configLoader.save(config);
            }
            config = configLoader.load();
        } catch (IOException e) {
            logger.error("Failed to load config", e);
        }
    }

    private void saveConfig() {
        try {
            configLoader.save(config);
        } catch (IOException e) {
            logger.error("Failed to save config", e);
        }
    }

    private void loadTokens() {
        tokens.clear();
        ConfigurationNode tokensNode = config.node("Tokens");
        for (Object keyObj : tokensNode.childrenMap().keySet()) {
            String key = keyObj.toString();
            String token = tokensNode.node(key).getString();
            if (token != null) {
                tokens.put(key, TokenUtil.createKeyFrom(token));
            }
        }
    }

    public static class VotifierEvent extends AbstractEvent {

        private final Vote vote;
        private final Cause cause;

        public VotifierEvent(Vote vote) {
            this.vote = vote;
            this.cause = Cause.of(EventContext.empty(), vote);
        }

        public Vote getVote() {
            return vote;
        }

        @Override
        public Cause cause() {
            return cause;
        }
    }
}