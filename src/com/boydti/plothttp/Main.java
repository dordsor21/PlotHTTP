package com.boydti.plothttp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.boydti.plothttp.object.ClusterResource;
import com.boydti.plothttp.object.CommentResource;
import com.boydti.plothttp.object.PlotResource;
import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.SchematicResource;
import com.boydti.plothttp.object.UUIDResource;
import com.boydti.plothttp.object.WebResource;
import com.boydti.plothttp.object.WorldResource;
import com.boydti.plothttp.util.NanoHTTPD;
import com.boydti.plothttp.util.RequestManager;
import com.boydti.plothttp.util.ResourceManager;
import com.boydti.plothttp.util.ServerRunner;

public class Main extends JavaPlugin {
    
    public static NanoHTTPD server;
    public static FileConfiguration config;
    public static Main plugin;
    
    public static int port = 8080;
    
    @Override
    public void onEnable() {
        Main.config = getConfig();
        Main.plugin = this;
        
        // Setting up configuration
        setupConfig();
        
        // Setting up resources
        setupResources();
        
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
    
    public void setupResources() {
        // Adding the plot resource
        ResourceManager.addResource(new ClusterResource());
        ResourceManager.addResource(new CommentResource());
        ResourceManager.addResource(new SchematicResource());
        ResourceManager.addResource(new UUIDResource());
        ResourceManager.addResource(new WebResource());
        ResourceManager.addResource(new WorldResource());
        ResourceManager.addResource(new PlotResource());
        
        
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
        if (Main.config.getBoolean("whitelist.enabled")) {
            for (String ip : config.getStringList("whitelist.allowed")) {
                HashMap<String, String> params = new HashMap<>();
                params.put("*", "*");
                RequestManager.addToken(new Request(ip, "*", "*", params));
            }
        }
        else {
            HashMap<String, String> params = new HashMap<>();
            params.put("*", "*");
            RequestManager.addToken(new Request("*", "*", "*", params));
        }
        Main.port = config.getInt("port");
        plugin.saveConfig();
    }
}