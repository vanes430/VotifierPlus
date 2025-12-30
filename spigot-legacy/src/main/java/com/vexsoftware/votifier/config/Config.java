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
                writer.println("# +-------------------------------------------------------------------------+");
                writer.println("# |                      VotifierPlus Configuration                         |");
                writer.println("# +-------------------------------------------------------------------------+");
                writer.println("");
                writer.println("# The IP address to listen on. 0.0.0.0 listens on all available interfaces.");
                writer.println("Host: 0.0.0.0");
                writer.println("");
                writer.println("# The port to listen on. Default is 8192.");
                writer.println("# Make sure this port is open in your firewall (TCP).");
                writer.println("Port: 8192");
                writer.println("");
                writer.println("# Debug levels: NONE, INFO, EXTRA.");
                writer.println("# INFO will show basic vote logging, EXTRA will show data packets.");
                writer.println("DebugLevel: NONE");
                writer.println("");
                writer.println("# Experimental: Enable V2 Token support (NuVotifier compatible).");
                writer.println("TokenSupport: false");
                writer.println("");
                writer.println("# Tokens for V2 authentication.");
                writer.println("Tokens:");
                writer.println("  default: '" + TokenUtil.newToken() + "'");
                writer.println("");
                writer.println("# Automatically clear the offline waiting list every X seconds.");
                writer.println("# Default: 7200 (2 hours). Set to 0 to disable.");
                writer.println("AutoClearDelay: 7200");
                writer.println("");
                writer.println("# Vote Forwarding: Send received votes to other servers.");
                writer.println("Forwarding:");
                writer.println("  server1:");
                writer.println("    # Whether forwarding to this server is enabled.");
                writer.println("    Enabled: false");
                writer.println("    # The IP address of the target server.");
                writer.println("    Host: '127.0.0.1'");
                writer.println("    # The port of the target server's VotifierPlus.");
                writer.println("    Port: 8193");
                writer.println("    # RSA Public Key of the target server (required if UseToken is false).");
                writer.println("    Key: ''");
                writer.println("    # Token used for V2 authentication.");
                writer.println("    # Leave empty to generate a new token automatically.");
                writer.println("    Token: ''");
                writer.println("    # Use V2 Token authentication instead of RSA keys.");
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

        this.host = config.getString("Host", "0.0.0.0");
        this.port = config.getInt("Port", 8192);
        
        String debugStr = config.getString("DebugLevel", "NONE");
        this.debug = debugStr.equalsIgnoreCase("INFO") || debugStr.equalsIgnoreCase("EXTRA") || debugStr.equalsIgnoreCase("DEV") || config.getBoolean("Debug", false);

        ConfigurationSection forwardingSection = config.getConfigurationSection("Forwarding");
        if (forwardingSection != null) {
            this.servers = forwardingSection.getKeys(false);
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