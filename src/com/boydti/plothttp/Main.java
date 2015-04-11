package com.boydti.plothttp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import fi.iki.elonen.ServerRunner;

public class Main extends JavaPlugin {
    
    public static NanoHTTPD server;
    public static FileConfiguration config;
    public static Main plugin;
    
    public static HashSet<String> allowedIps = new HashSet<>();
    public static boolean content = true;
    public static boolean api = true;
    public static boolean whitelist = true;
    public static int port = 8080;
    
    @Override
    public void onEnable() {
        Main.config = getConfig();
        Main.plugin = this;
        
        // Setting up configuration
        setupConfig();
        
        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                Main.server = ServerRunner.run(PlotServer.class);
            }
        });
    }
    
    @Override
    public void onDisable() {
        Main.server.stop();
    }
    
    public void setupConfig() {
        if (Main.config == null) {
            plugin.saveDefaultConfig();
        }
        final Map<String, Object> options = new HashMap<>();
        
        Main.config = plugin.getConfig();
        Main.config.set("version", Main.plugin.getDescription().getVersion());
        
        options.put("whitelist.enabled", true);
        options.put("whitelist.allowed", new String[] { "127.0.0.1" });
        options.put("content.serve", true);
        options.put("api.serve", true);
        options.put("port", 8080);
        
        for (final Entry<String, Object> node : options.entrySet()) {
            if (!config.contains(node.getKey())) {
                config.set(node.getKey(), node.getValue());
            }
        }
        Main.whitelist = Main.config.getBoolean("whitelist.enabled");
        if (whitelist) {
            allowedIps.addAll(config.getStringList("whitelist.allowed"));
        }
        Main.content = config.getBoolean("conent.serve");
        Main.api = config.getBoolean("api.serve");
        Main.port = config.getInt("port");
        plugin.saveConfig();
    }
}