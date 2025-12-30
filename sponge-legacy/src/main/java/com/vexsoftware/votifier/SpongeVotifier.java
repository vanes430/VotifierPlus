package com.vexsoftware.votifier;

import com.google.inject.Inject;
import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.crypto.TokenUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VoteReceiver;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.util.*;

@Plugin(id = "votifierplus", name = "VotifierPlus", version = "2.0.0-SNAPSHOT", description = "A plugin that gets notified when votes are made for the server on toplists.", authors = {"vanes430"})
public class SpongeVotifier {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configLoader;

    private ConfigurationNode config;
    private VoteReceiver voteReceiver;
    private KeyPair keyPair;
    private Map<String, Key> tokens = new HashMap<>();

    @Listener
    public void onInit(GameInitializationEvent event) {
        logger.info("Initializing VotifierPlus for Sponge...");

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

        String host = config.getNode("Host").getString("0.0.0.0");
        int port = config.getNode("Port").getInt(8192);

        if (port == 0) {
            try {
                ServerSocket s = new ServerSocket();
                s.bind(new InetSocketAddress(host, 0));
                port = s.getLocalPort();
                s.close();
                config.getNode("Port").setValue(port);
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
                    return config.getNode("TokenSupport").getBoolean(false);
                }

                @Override
                public String getVersion() {
                    return "Sponge-2.0.0-SNAPSHOT";
                }

                @Override
                public Map<String, Key> getTokens() {
                    return tokens;
                }

                @Override
                public Set<String> getServers() {
                    ConfigurationNode forwardingNode = config.getNode("Forwarding");
                    if (forwardingNode.isVirtual() || !forwardingNode.hasMapChildren()) {
                        return Collections.emptySet();
                    }
                    Set<String> servers = new HashSet<>();
                    for (Object key : forwardingNode.getChildrenMap().keySet()) {
                        servers.add(key.toString());
                    }
                    return servers;
                }

                @Override
                public ForwardServer getServerData(String s) {
                    ConfigurationNode node = config.getNode("Forwarding", s);
                    if (node.isVirtual()) return null;

                    boolean enabled = node.getNode("Enabled").getBoolean(false);
                    String host = node.getNode("Host").getString("");
                    int port = node.getNode("Port").getInt(8192);
                    String key = node.getNode("Key").getString("");
                    String token = node.getNode("Token").getString("");
                    boolean useToken = node.getNode("UseToken").getBoolean(false);

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
                    if (config.getNode("Debug").getBoolean(false)) {
                        logger.info("[DEBUG] " + msg);
                    }
                }

                @Override
                public void debug(Exception e) {
                    if (config.getNode("Debug").getBoolean(false)) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void callEvent(Vote vote) {
                    Task.builder().execute(() -> {
                         Sponge.getEventManager().post(new VotifierEvent(vote));
                    }).submit(SpongeVotifier.this);
                }
            };
            voteReceiver.start();
            logger.info("VotifierPlus loaded on port " + finalPort + ".");
        } catch (Exception e) {
            logger.error("Failed to start VoteReceiver", e);
        }
    }

    @Listener
    public void onStop(GameStoppingEvent event) {
        if (voteReceiver != null) {
            voteReceiver.shutdown();
        }
        logger.info("VotifierPlus disabled.");
    }

    private void loadConfig() {
        try {
            if (!new File(configDir.toFile(), "votifierplus.conf").exists()) {
                config = configLoader.createEmptyNode();
                config.getNode("Host").setValue("0.0.0.0");
                config.getNode("Port").setValue(8192);
                config.getNode("Debug").setValue(false);
                config.getNode("TokenSupport").setValue(false);
                config.getNode("Tokens", "default").setValue(TokenUtil.newToken());
                
                // Example forwarding
                ConfigurationNode forwarding = config.getNode("Forwarding", "example");
                forwarding.getNode("Enabled").setValue(false);
                forwarding.getNode("Host").setValue("127.0.0.1");
                forwarding.getNode("Port").setValue(8193);
                forwarding.getNode("Key").setValue("");
                forwarding.getNode("Token").setValue("");
                forwarding.getNode("UseToken").setValue(false);
                
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
        ConfigurationNode tokensNode = config.getNode("Tokens");
        for (Object keyObj : tokensNode.getChildrenMap().keySet()) {
            String key = keyObj.toString();
            String token = tokensNode.getNode(key).getString();
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
        public Cause getCause() {
            return cause;
        }
    }
}
