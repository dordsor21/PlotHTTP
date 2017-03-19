package com.boydti.plothttp;

import com.boydti.plothttp.command.Upload;
import com.boydti.plothttp.command.Web;
import com.boydti.plothttp.object.ClusterResource;
import com.boydti.plothttp.object.CommentResource;
import com.boydti.plothttp.object.PlotResource;
import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.SchematicResource;
import com.boydti.plothttp.object.UUIDResource;
import com.boydti.plothttp.object.WebResource;
import com.boydti.plothttp.object.WorldResource;
import com.boydti.plothttp.util.RequestManager;
import com.boydti.plothttp.util.ResourceManager;
import com.boydti.plothttp.util.ServerRunner;
import com.boydti.plothttp.util.WebUtil;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.TaskManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    private static Main IMP;

    public File FILE = null;

    private PlotServer server;
    private WebSettings settings;

    private boolean commands = false;

    public static void deleteFolder(final File folder) {
        final File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (final File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public static Main imp() {
        return IMP;
    }

    public static WebSettings config() {
        return IMP.settings;
    }

    @Override
    public void onEnable() {
        IMP = this;
        try {
            FILE = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }

        // Clear downloads on load
        deleteFolder(new File(getDataFolder() + File.separator + "downloads"));

        // Setting up configuration
        setupConfig();

        // Setting up resources
        setupResources();

        // Setup web interface
        setupWeb();

        // Setup commands
        setupCommands();

        // Setup events
        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                server = ServerRunner.run(PlotServer.class);
                RequestManager manager = server.getRequestManager();
                if (config().WHITELIST.ENABLED) {
                    for (final String ip : config().WHITELIST.ALLOWED) {
                        final HashMap<String, String> params = new HashMap<>();
                        params.put("*", "*");
                        manager.addToken(new Request(ip, "*", "*", params));
                    }
                } else {
                    final HashMap<String, String> params = new HashMap<>();
                    params.put("*", "*");
                    manager.addToken(new Request("*", "*", "*", params));
                }
            }
        });
        saveResource("level.dat", false);

        TaskManager.IMP.taskRepeat(new Runnable() {
            @Override
            public void run() {
                unload();
            }
        }, 20);
    }

    private void unload() {
        PlotArea area = PS.get().getPlotArea("*", null);
        long start = System.currentTimeMillis();
        if (area != null) {
            Map<PlotId, Plot> plotsRaw = area.getPlotsRaw();
            for (Entry<PlotId, Plot> entry : plotsRaw.entrySet()) {
                Plot plot = entry.getValue();
                String worldName = plot.getId().x + "," + plot.getId().y;
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    List<PlotPlayer> players = plot.getPlayersInPlot();
                    if (players.isEmpty()) {
                        for (Chunk chunk : world.getLoadedChunks()) {
                            chunk.unload(true, false);
                            if (System.currentTimeMillis() - start > 50) {
                                break;
                            }
                        }
                        Bukkit.unloadWorld(world, false);
                    }
                }
            }
        }
    }

    public PlotServer getWebServer() {
        return server;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unload();
    }

    @Override
    public void onDisable() {
        getWebServer().getRequestManager().clear();
        server.stop();
    }

    public void setupCommands() {
        if (this.commands) {
            return;
        }
        this.commands = true;
        MainCommand.getInstance().addCommand(new Web());
        MainCommand.getInstance().register(new Upload());
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

    public void copyFile(final String file) {
        try {
            final byte[] buffer = new byte[2048];
            final File output = getDataFolder();
            if (!output.exists()) {
                output.mkdirs();
            }
            final File newFile = new File((output + File.separator + file));
            if (newFile.exists()) {
                return;
            }
            final ZipInputStream zis = new ZipInputStream(new FileInputStream(FILE));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                final String name = ze.getName();
                if (name.equals(file)) {
                    new File(newFile.getParent()).mkdirs();
                    final FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    ze = null;
                } else {
                    ze = zis.getNextEntry();
                }
            }
            zis.closeEntry();
            zis.close();
        } catch (final Exception e) {
            e.printStackTrace();
            PS.debug("Could not save " + file);
        }
    }

    public void setupWeb() {
        // Copy over template files
        copyFile("views/index.html");
        copyFile("views/upload.html");
        copyFile("views/uploadworld.html");
        copyFile("views/download.html");

        // Loading web files
        final File directory = new File(getDataFolder() + File.separator + "views");
        final File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.toLowerCase().endsWith(".html");
            }
        });
        for (final File file : files) {
            try {
                WebUtil.addPage(file.getName().substring(0, file.getName().length() - 5), new String(Files.readAllBytes(file.toPath())));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setupConfig() {
        File config = new File(getDataFolder(), "settings.yml");
        settings = new WebSettings();
        settings.load(config);
        settings.VERSION = getDescription().getVersion();
        settings.save(config);
    }
}
