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
        loadKeys();
        
        try {
            String host = (String) config.getOrDefault("host", "0.0.0.0");
            int port = (int) config.getOrDefault("port", 8192);

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
                     return false;
                }

                @Override
                public String getVersion() {
                    return "Standalone-1.0";
                }

                @Override
                public Map<String, Key> getTokens() {
                    return new HashMap<>();
                }

                @Override
                public Set<String> getServers() {
                    Map<String, Object> forwarding = (Map<String, Object>) config.get("forwarding");
                    if (forwarding == null) return Set.of();
                    return forwarding.keySet();
                }

                @Override
                public ForwardServer getServerData(String s) {
                    Map<String, Object> forwarding = (Map<String, Object>) config.get("forwarding");
                    if (forwarding == null) return null;
                    Map<String, Object> serverConfig = (Map<String, Object>) forwarding.get(s);
                    if (serverConfig == null) return null;

                    String address = (String) serverConfig.get("address"); // host:port
                    String[] split = address.split(":");
                    String host = split[0];
                    int port = Integer.parseInt(split[1]);
                    
                    String method = (String) serverConfig.getOrDefault("method", "pluginMessage"); // not used here really
                    String keyStr = (String) serverConfig.getOrDefault("key", "");
                    String tokenStr = (String) serverConfig.getOrDefault("token", "");
                    boolean enabled = (Boolean) serverConfig.getOrDefault("enabled", true);
                    boolean useToken = (Boolean) serverConfig.getOrDefault("usetoken", false);
                    
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
                    if (Boolean.TRUE.equals(config.get("debug"))) {
                        logger.info("[DEBUG] " + msg);
                    }
                }

                @Override
                public void debug(Exception e) {
                    if (Boolean.TRUE.equals(config.get("debug"))) {
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

    private void createDefaultConfig(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("host: 0.0.0.0\n");
            writer.write("port: 8192\n");
            writer.write("debug: false\n");
            writer.write("forwarding:\n");
            writer.write("  # lobby:\n");
            writer.write("  #   address: 127.0.0.1:8193\n");
            writer.write("  #   # key: PUBLIC_KEY_HERE (Optional if using tokens)\n");
            writer.write("  #   token: TOKEN_HERE\n");
            writer.write("  #   usetoken: false\n");
            writer.write("  #   enabled: true\n");
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
