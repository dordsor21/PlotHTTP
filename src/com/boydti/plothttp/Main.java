package com.boydti.plothttp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import com.boydti.plothttp.util.WebUtil;
import com.intellectualcrafters.plot.PlotSquared;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.commands.SubCommand.CommandCategory;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;

public class Main extends JavaPlugin {
    
    public static NanoHTTPD server;
    public static FileConfiguration config;
    public static Main plugin;
    
    public static int port = 8080;
    public static File FILE = null;
    
    private boolean commands = false;
    
    @Override
    public void onEnable() {
        Main.config = getConfig();
        Main.plugin = this;
        try {
            Main.FILE = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        
        // Setting up configuration
        setupConfig();
        
        // Setting up resources
        setupResources();
        
        // Setup web interface
        setupWeb();
        
        // Setup commands
        setupCommands();
        
        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                Main.server = ServerRunner.run(PlotServer.class);
            }
        });
    }
    
    @Override
    public void onDisable() {
        for (Request token : RequestManager.getTokens()) {
            RequestManager.removeToken(token);
        }
        Main.server.stop();
    }
    
    
    public void setupCommands() {
        if (commands) {
            return;
        }
        commands = true;
        SubCommand cmd = new SubCommand("web", "plots.admin", "Web related commands", "", "web", CommandCategory.DEBUG, false) {
            @Override
            public boolean execute(PlotPlayer player, String... args) {
                if (player != null) {
                    MainUtil.sendMessage(player, C.NOT_CONSOLE);
                    return false;
                }
                if (args.length == 0) {
                    MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot web reload");
                    return false;
                }
                switch (args[0]) {
                    case "reload": {
                        onDisable();
                        onEnable();
                        MainUtil.sendMessage(player, "&aReloaded success!");
                        return true;
                    }
                    default: {
                        MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot web reload");
                    }
                }
                return false;
            }
        };
        MainCommand.subCommands.add(cmd);
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
    
    public void copyFile(String file) {
        try {
            byte[] buffer = new byte[2048];
            File output = plugin.getDataFolder();
            if (!output.exists()) {
                output.mkdirs();
            }
            File newFile = new File((output + File.separator + "web" + File.separator + file));
            if (newFile.exists()) {
                return;
            }
            ZipInputStream zis = new ZipInputStream(new FileInputStream(FILE));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String name = ze.getName();
                if (name.equals(file)) {
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    ze = null;
                }
                else {
                    ze = zis.getNextEntry();
                }
            }
            zis.closeEntry();
            zis.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.print("Could not save " + file);
        }
    }
    
    public void setupWeb() {
        // Copy over index.html
        copyFile("index.html");
        
        // Loading web files
        File directory = new File(plugin.getDataFolder() + File.separator + "web");
        File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".html");
            }
        });
        for (File file : files) {
            try {
                WebUtil.addPage(file.getName().substring(0, file.getName().length() - 5), new String(Files.readAllBytes(file.toPath())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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