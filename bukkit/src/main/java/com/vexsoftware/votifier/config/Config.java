package com.vexsoftware.votifier.config;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.vexsoftware.votifier.VotifierPlus;
import com.vexsoftware.votifier.crypto.TokenUtil;

import lombok.Getter;

public class Config {

    private final VotifierPlus plugin;

    @Getter
    private String host;
    @Getter
    private int port;
    
    private boolean debug;
    @Getter
    private Set<String> servers;
    @Getter
    private String formatNoPerms;
    @Getter
    private String formatNotNumber;
    @Getter
    private String helpLine;
    @Getter
    private boolean disableUpdateChecking;
    @Getter
    private boolean tokenSupport;
    @Getter
    private int autoClearDelay;

    public Config(VotifierPlus plugin) {
        this.plugin = plugin;
        setup();
        loadValues();
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        java.io.File file = new java.io.File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.getLogger().info("Generating config.yml...");
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file))) {
                writer.println("# Debug levels:");
                writer.println("# NONE");
                writer.println("# INFO");
                writer.println("# EXTRA");
                writer.println("DebugLevel: NONE");
                writer.println("");
                writer.println("# The host VotifierPlus will listen on");
                writer.println("host: 0.0.0.0");
                writer.println("");
                writer.println("# The port VotifierPlus will listen on");
                writer.println("port: 8192");
                writer.println("");
                writer.println("# This is still new to VotifierPlus, so it's disabled by default.");
                writer.println("TokenSupport: false");
                writer.println("");
                writer.println("# Auto-clear waiting list every X seconds (Default: 7200 = 2 hours)");
                writer.println("# Set to 0 to disable.");
                writer.println("AutoClearDelay: 7200");
                writer.println("");
                writer.println("# Vote Forwarding Configuration");
                writer.println("Forwarding:");
                writer.println("  server1:");
                writer.println("    Enabled: false");
                writer.println("    Host: '127.0.0.1'");
                writer.println("    Port: 8193");
                writer.println("    Key: ''");
                writer.println("    # Token used for authentication.");
                writer.println("    # Leave empty to generate a new token automatically.");
                writer.println("    Token: ''");
                writer.println("    # Use token for authentication instead of RSA key.");
                writer.println("    UseToken: false");
            } catch (java.io.IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not generate config.yml", e);
            }
        }
        plugin.reloadConfig();
    }

    public void reloadData() {
        plugin.reloadConfig();
        loadValues();
    }
    
    public FileConfiguration getData() {
        return plugin.getConfig();
    }
    
    public void saveData() {
        plugin.saveConfig();
    }
    
    // Helper to simulate "isJustCreated" if needed, though usually standard Bukkit doesn't check this explicitly after saveDefaultConfig
    public boolean isJustCreated() {
        return false; 
    }
    
    // Helper to set values
    public void setValue(String path, Object value) {
        plugin.getConfig().set(path, value);
        saveData();
    }

    public void loadValues() {
        FileConfiguration config = plugin.getConfig();

        this.host = config.getString("host", "0.0.0.0");
        this.port = config.getInt("port", 8192);
        
        String debugStr = config.getString("DebugLevel", "NONE");
        this.debug = debugStr.equalsIgnoreCase("INFO") || debugStr.equalsIgnoreCase("EXTRA") || debugStr.equalsIgnoreCase("DEV") || config.getBoolean("debug", false);

        ConfigurationSection forwardingSection = config.getConfigurationSection("Forwarding");
        if (forwardingSection != null) {
            this.servers = forwardingSection.getKeys(false);
            boolean changed = false;
            for (String server : this.servers) {
                String token = forwardingSection.getString(server + ".Token");
                if (token == null || token.isEmpty()) {
                    String newToken = TokenUtil.newToken();
                    forwardingSection.set(server + ".Token", newToken);
                    // Add a comment or info if possible, but Bukkit API doesn't support comments easily in logic
                    plugin.getLogger().info("Generated new token for forwarding server '" + server + "': " + newToken);
                    changed = true;
                }
            }
            if (changed) {
                saveData();
            }
        } else {
            this.servers = new HashSet<>();
        }

        this.formatNoPerms = config.getString("Format.NoPerms", "&cYou do not have enough permission!");
        this.formatNotNumber = config.getString("Format.NotNumber", "&cError on &6%arg%&c, number expected!");
        this.helpLine = config.getString("Format.HelpLine", "&3&l%Command% - &3%HelpMessage%");
        this.disableUpdateChecking = config.getBoolean("DisableUpdateChecking", false);
        this.tokenSupport = config.getBoolean("TokenSupport", false);
        this.autoClearDelay = config.getInt("AutoClearDelay", 7200);
    }

    public ConfigurationSection getForwardingConfiguration(String s) {
        return plugin.getConfig().getConfigurationSection("Forwarding." + s);
    }
    
    // Compatibility helper for getting DebugLevel-like boolean check
    public boolean isDebug() {
        return debug;
    }
    
    public boolean isDebug(Object level) {
         // simplified logic, if debug is on, return true
         return debug;
    }
}