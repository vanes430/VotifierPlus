package com.vexsoftware.votifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.yaml.snakeyaml.Yaml;

import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.crypto.TokenUtil;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VoteReceiver;

import lombok.Getter;

public class StandaloneMain {

    @Getter
    private static StandaloneMain instance;
    private Logger logger = Logger.getLogger("VotifierPlus");
    private Map<String, Object> config;
    private VoteReceiver voteReceiver;
    private KeyPair keyPair;
    private Map<String, Key> tokens = new HashMap<>();
    private long startTime;

    public static void main(String[] args) {
        new StandaloneMain().start();
    }

    public void start() {
        setupLogger();
        instance = this;
        startTime = System.currentTimeMillis();
        logger.info("Starting VotifierPlus Standalone...");

        loadConfig();
        loadTokens();
        loadKeys();
        
        try {
            String host = (String) config.getOrDefault("Host", "0.0.0.0");
            int port = (int) config.getOrDefault("Port", 8192);

            voteReceiver = new VoteReceiver(host, port) {

                @Override
                public void logWarning(String warn) {
                    logger.warning(warn);
                }

                @Override
                public void logSevere(String msg) {
                    logger.severe(msg);
                }

                @Override
                public void log(String msg) {
                    logger.info(msg);
                }

                @Override
                public boolean isUseTokens() {
                     return (Boolean) config.getOrDefault("TokenSupport", false);
                }

                @Override
                public String getVersion() {
                    return "Standalone-1.0";
                }

                @Override
                public Map<String, Key> getTokens() {
                    return tokens;
                }

                @Override
                public Set<String> getServers() {
                    Map<String, Object> forwarding = (Map<String, Object>) config.get("Forwarding");
                    if (forwarding == null) return Set.of();
                    return forwarding.keySet();
                }

                @Override
                public ForwardServer getServerData(String s) {
                    Map<String, Object> forwarding = (Map<String, Object>) config.get("Forwarding");
                    if (forwarding == null) return null;
                    Map<String, Object> serverConfig = (Map<String, Object>) forwarding.get(s);
                    if (serverConfig == null) return null;

                    String address = (String) serverConfig.getOrDefault("Address", "127.0.0.1:8192"); // host:port
                    String[] split = address.split(":");
                    String host = split[0];
                    int port = Integer.parseInt(split[1]);
                    
                    String method = (String) serverConfig.getOrDefault("Method", "pluginMessage"); // not used here really
                    String keyStr = (String) serverConfig.getOrDefault("Key", "");
                    String tokenStr = (String) serverConfig.getOrDefault("Token", "");
                    boolean enabled = (Boolean) serverConfig.getOrDefault("Enabled", true);
                    boolean useToken = (Boolean) serverConfig.getOrDefault("UseToken", false);
                    
                    Key tokenKey = null;
                    if (tokenStr != null && !tokenStr.isEmpty()) {
                        tokenKey = TokenUtil.createKeyFrom(tokenStr);
                    }
                    
                    return new ForwardServer(enabled, host, port, keyStr, tokenKey, useToken);
                }

                @Override
                public KeyPair getKeyPair() {
                    return keyPair;
                }

                @Override
                public void debug(String msg) {
                    if (Boolean.TRUE.equals(config.get("Debug"))) {
                        logger.info("[DEBUG] " + msg);
                    }
                }

                @Override
                public void debug(Exception e) {
                    if (Boolean.TRUE.equals(config.get("Debug"))) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void callEvent(Vote e) {
                    logger.info("Received vote from " + e.getUsername() + " service: " + e.getServiceName());
                }
            };
            voteReceiver.start();
            logger.info("VotifierPlus Standalone started on " + host + ":" + port);
            
            handleCommands();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void setupLogger() {
        java.util.logging.ConsoleHandler handler = new java.util.logging.ConsoleHandler();
        handler.setFormatter(new java.util.logging.Formatter() {
            @Override
            public String format(java.util.logging.LogRecord record) {
                return String.format("[%s] [%s] %s%n", 
                    "VotifierPlus", 
                    record.getLevel().getLocalizedName(), 
                    record.getMessage());
            }
        });
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
    }

    private void handleCommands() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        while (true) {
            String input = scanner.nextLine().toLowerCase();
            if (input.equals("stop")) {
                logger.info("Stopping VotifierPlus Standalone...");
                if (voteReceiver != null) voteReceiver.shutdown();
                System.exit(0);
            } else if (input.equals("uptime")) {
                long diff = System.currentTimeMillis() - startTime;
                long seconds = (diff / 1000) % 60;
                long minutes = (diff / (1000 * 60)) % 60;
                long hours = (diff / (1000 * 60 * 60)) % 24;
                long days = (diff / (1000 * 60 * 60 * 24));
                logger.info(String.format("Uptime: %d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds));
            } else if (input.equals("status")) {
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory();
                long allocatedMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                
                logger.info("--- JVM Status ---");
                logger.info("Memory (Max): " + (maxMemory / 1024 / 1024) + "MB");
                logger.info("Memory (Allocated): " + (allocatedMemory / 1024 / 1024) + "MB");
                logger.info("Memory (Free): " + (freeMemory / 1024 / 1024) + "MB");
                logger.info("Available Processors: " + runtime.availableProcessors());
                
                try {
                    java.lang.management.OperatingSystemMXBean osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
                    logger.info("System Load Average: " + osBean.getSystemLoadAverage());
                } catch (Exception e) {}
                logger.info("------------------");
            } else {
                logger.info("Available commands: uptime, status, stop");
            }
        }
    }

    private void loadConfig() {
        File configFile = new File("config.yml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }
        try (InputStream in = new FileInputStream(configFile)) {
            config = new Yaml().load(in);
        } catch (Exception e) {
            e.printStackTrace();
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
        File configFile = new File("config.yml");
        try (FileWriter writer = new FileWriter(configFile)) {
            new Yaml().dump(config, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("# +-------------------------------------------------------------------------+\n");
            writer.write("# |                  VotifierPlus Standalone Configuration                  |\n");
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
            writer.write("# Vote Forwarding: Send votes received by this application to other servers.\n");
            writer.write("Forwarding:\n");
            writer.write("  lobby:\n");
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadKeys() {
        File rsaDir = new File("rsa");
        try {
            if (!rsaDir.exists()) {
                rsaDir.mkdir();
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDir, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
