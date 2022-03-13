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
import com.boydti.plothttp.util.FileUtil;
import com.boydti.plothttp.util.Logger;
import com.boydti.plothttp.util.RequestManager;
import com.boydti.plothttp.util.ResourceManager;
import com.boydti.plothttp.util.ServerRunner;
import com.boydti.plothttp.util.WebUtil;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.TaskManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;

public class Main {

    private static Main IMP;

    public final File DIR;
    public final File FILE;

    private PlotServer server;
    private WebSettings settings;

    private boolean commands = false;
    private Logger logger;

    public Main() throws URISyntaxException, MalformedURLException {
        IMP = this;
        URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();
        FILE = new File(new URL(url.toURI().toString().split("\\!")[0].replaceAll("jar:file", "file")).toURI().getPath());
        DIR = new File(FILE.getParentFile(), "PlotHTTP");
    }

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

    public void open() {

        File downloads = new File(DIR + File.separator + "downloads");
        // Clear downloads on load
        deleteFolder(downloads);
        downloads.mkdir();

        // Setting up configuration
        setupConfig();

        // Setting up resources
        setupResources();

        // Setup web interface
        setupWeb();

        // Setup commands
        setupCommands();

        TaskManager.IMP.taskAsync(() -> {
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
        });
        if (!DIR.exists()) {
            DIR.mkdirs();
        }
        FileUtil.copyFile(FILE, "level.dat", DIR);

        setupLogger();
    }

    private void setupLogger() {
        try {
            if (this.logger != null) {
                this.logger.close();
            }
            File file = MainUtil.getFile(DIR, settings.LOG_FILE);
            if (!file.exists()) {
                file.createNewFile();
            }
            this.logger = new Logger(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public PlotServer getWebServer() {
        return server;
    }

    public void close() {
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

    public void setupWeb() {
        FileUtil.copyFile(FILE, "views/index.html", DIR);
        FileUtil.copyFile(FILE, "views/upload.html", DIR);
        FileUtil.copyFile(FILE, "views/uploadworld.html", DIR);
        FileUtil.copyFile(FILE, "views/download.html", DIR);

        // Loading web files
        final File directory = new File(DIR + File.separator + "views");
        final File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".html"));
        for (final File file : files) {
            try {
                WebUtil.addPage(file.getName().substring(0, file.getName().length() - 5), new String(Files.readAllBytes(file.toPath())));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setupConfig() {
        File config = new File(DIR, "settings.yml");
        settings = new WebSettings();
        settings.load(config);
        settings.VERSION = "development";
        settings.save(config);
    }
}
