package com.boydti.plothttp.command;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.WebResource;
import com.boydti.plothttp.util.WorldUtil;
import com.intellectualcrafters.configuration.file.YamlConfiguration;
import com.intellectualcrafters.jnbt.CompoundTag;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.object.worlds.SinglePlotArea;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.SchematicHandler;
import com.intellectualcrafters.plot.util.StringMan;
import com.intellectualcrafters.plot.util.TaskManager;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.general.commands.CommandDeclaration;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@CommandDeclaration(
        command = "web",
        permission = "plots.web",
 category = CommandCategory.SCHEMATIC,
        requiredType = RequiredType.NONE,
        description = "Web related commands",
        usage = "/plots web"
)
public class Web extends SubCommand {

    public void noargs(final PlotPlayer player) {
        final ArrayList<String> args = new ArrayList<String>();
        if (Permissions.hasPermission(player, "plots.web.reload")) {
            args.add("reload");
        }
        if (Permissions.hasPermission(player, "plots.web.download")) {
            args.add("download");
        }
        if (Permissions.hasPermission(player, "plots.web.upload")) {
            args.add("upload");
        }
        if (args.size() == 0) {
            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.reload");
            return;
        }
        MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot web <" + StringMan.join(args, "|") + ">");
    }

    public static List<File> getFiles(final File root, final List<File> files) {
        final File[] list = root.listFiles();
        if (list == null) {
            return files;
        }
        for (final File f : list) {
            if (f.isDirectory()) {
                return getFiles(f, files);
            } else {
                if (f.getName().endsWith(".schematic")) {
                    files.add(f);
                }
            }
        }
        return files;
    }

    private final HashMap<String, Long> timestamps = new HashMap<>();

    @Override
    public boolean onCommand(final PlotPlayer player, final String... args) {
        if (args.length == 0) {
            noargs(player);
            return false;
        }
        switch (args[0]) {
            case "reload": {
                if (!Permissions.hasPermission(player, "plots.web.reload")) {
                    MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.reload");
                    return false;
                }
                Main.imp().close();
                Main.imp().open();
                MainUtil.sendMessage(player, "&aReloaded success!");
                return true;
            }
            case "download": {
                if (!Permissions.hasPermission(player, "plots.web.download")) {
                    MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.download");
                    return false;
                }
                if (args.length < 2) {
                    MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot web download <schematic|world|worldedit>");
                    return false;
                }
                final Plot plot;
                switch (args[1].toLowerCase()) {
                    case "worldedit": {
                        plot = null;
                        break;
                    }
                    case "world":
                    case "schematic": {
                        plot = player.getCurrentPlot();
                        if ((plot == null) || (!plot.isAdded(player.getUUID()) && !Permissions.hasPermission(player, "plots.web.download.other"))) {
                            MainUtil.sendMessage(player, C.NO_PLOT_PERMS);
                            return false;
                        }
                        break;
                    }
                    default: {
                        MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot web download <schematic|world|worldedit>");
                        return false;
                    }
                }
                final Long last = this.timestamps.get(player.getName());
                if (last != null) {
                    if (last < 600000) {
                        final int left = (int) ((System.currentTimeMillis() - last) / 60000);
                        MainUtil.sendMessage(player, "&cYou must wait " + left + " minutes before another download!");
                        return false;
                    } else {
                        this.timestamps.remove(player.getName());
                    }
                }
                final String id = WebResource.nextId();
                final String port;
                if (Main.config().PORT != 80) {
                    port = ":" + Main.config().PORT;
                } else {
                    port = "";
                }
                if (!Permissions.hasPermission(player, "plots.web.download." + args[1].toLowerCase())) {
                    MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.download." + args[1].toLowerCase());
                    return false;
                }
                MainUtil.sendMessage(player, "&6Please wait while we process your plot...");
                switch (args[1].toLowerCase()) {
                    case "worldedit": {
                        if (!Permissions.hasPermission(player, "plots.web.download.worldedit")) {
                            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.download.worldedit");
                            return false;
                        }
                        final File worldeditDir = new File("plugins" + File.separator + "WorldEdit");
                        final File worldeditFile = new File(worldeditDir + File.separator + "config.yml");
                        if (!worldeditFile.exists()) {
                            MainUtil.sendMessage(player, "WorldEdit not found!");
                            return false;
                        }
                        final YamlConfiguration config = YamlConfiguration.loadConfiguration(worldeditFile);
                        final File savePath = new File(config.getString("saving.dir"));
                        File directory;
                        if (savePath.isAbsolute()) {
                            directory = savePath;
                        } else {
                            directory = new File(worldeditDir + File.separator + config.getString("saving.dir"));
                        }
                        if (args.length != 3) {
                            final List<File> files = getFiles(directory, new ArrayList<File>());
                            for (final File file : files) {
                                MainUtil.sendMessage(player, file.getName());
                            }
                            MainUtil.sendMessage(player, "&6Please specify a file: /plot web download worldedit <file>");
                            return false;
                        }
                        final File schem = new File(directory, args[2]);
                        if (!schem.exists()) {
                            final List<File> files = getFiles(directory, new ArrayList<File>());
                            for (final File file : files) {
                                MainUtil.sendMessage(player, file.getName());
                            }
                            MainUtil.sendMessage(player, "&6Please specify a file: /plot web download worldedit <file>");
                            return false;
                        }
                        TaskManager.runTaskAsync(new Runnable() {
                            @Override
                            public void run() {
                                MainUtil.sendMessage(player, "&6Generating link...");
                                WebResource.downloads.put(id, schem.getAbsolutePath());
                                WebResource.downloadsUUID.put(id, player.getUUID());
                                MainUtil.sendMessage(player, "Download the file: " + Main.config().WEB_IP + port + "/web?id=" + id);
                                final HashMap<String, String> map = new HashMap<>();
                                map.put("id", id);
                                final Request r = new Request("*", "GET", "/web", map, 2);
                                Main.imp().getWebServer().getRequestManager().addToken(r);
                            }
                        });
                        this.timestamps.put(player.getName(), System.currentTimeMillis());
                        return true;
                    }
                    case "world": {
                        if (!Permissions.hasPermission(player, "plots.web.download.world")) {
                            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.download.world");
                            return false;
                        }
                        final String owner = UUIDHandler.getName(plot.owner);
                        if (owner == null) {
                            MainUtil.sendMessage(player, "&7(invalid owner) Could not export &c" + plot.getId());
                        }
                        String worldName = (plot.getArea() instanceof SinglePlotArea) ? plot.getId().toString().replace(';', ',') : plot.getArea().worldname;
                        final String filename = plot.getId().x + "," + plot.getId().y + "," + worldName + "," + owner + ".zip";
                        com.intellectualcrafters.plot.util.WorldUtil.IMP.saveWorld(worldName);
                        boolean result;
                        try {
                            result = WorldUtil.save(plot, filename);
                        } catch (final Exception e) {
                            e.printStackTrace();
                            result = false;
                        }
                        if (result) {
                            MainUtil.sendMessage(player, "&6Generating link... (this will take 10 seconds)");
                            WebResource.downloads.put(id, filename);
                            WebResource.downloadsUUID.put(id, player.getUUID());
                            TaskManager.runTaskLater(new Runnable() {
                                @Override
                                public void run() {
                                    MainUtil.sendMessage(player, "Download the file: " + Main.config().WEB_IP + port + "/web?id=" + id);
                                    final HashMap<String, String> map = new HashMap<>();
                                    map.put("id", id);
                                    final Request r = new Request("*", "GET", "/web", map, 2);
                                    Main.imp().getWebServer().getRequestManager().addToken(r);
                                }
                            }, 200);
                            this.timestamps.put(player.getName(), System.currentTimeMillis());
                            return true;
                        } else {
                            MainUtil.sendMessage(player, "&7Could not export &c" + plot.getId());
                            return false;
                        }
                    }
                    case "schematic": {
                        if (!Permissions.hasPermission(player, "plots.web.download.schematic")) {
                            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.download.schematic");
                            return false;
                        }
                        MainUtil.sendMessage(player, "&6Processing plot...");
                        SchematicHandler.manager.getCompoundTag(plot, new RunnableVal<CompoundTag>() {
                            @Override
                            public void run(final CompoundTag value) {
                                final String o = UUIDHandler.getName(plot.owner);
                                final String owner = o == null ? "unknown" : o;
                                if (value == null) {
                                    MainUtil.sendMessage(player, "&7Could not export &c" + plot.getId());
                                    return;
                                }
                                TaskManager.runTaskAsync(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainUtil.sendMessage(player, "&6Generating link...");
                                        final String filename = plot.getId().x + ";" + plot.getId().y + "," + plot.getArea() + "," + owner + ".schematic";
                                        final boolean result = SchematicHandler.manager.save(value, Main.imp().DIR + File.separator + "downloads" + File.separator + filename);
                                        if (!result) {
                                            MainUtil.sendMessage(player, "&7Could not export &c" + plot.getId());
                                        } else {
                                            MainUtil.sendMessage(null, "&7 - &a  success: " + plot.getId());
                                            WebResource.downloads.put(id, filename);
                                            WebResource.downloadsUUID.put(id, player.getUUID());
                                            MainUtil.sendMessage(player, "Download the file: " + Main.config().WEB_IP + port + "/web?id=" + id);
                                            final HashMap<String, String> map = new HashMap<>();
                                            map.put("id", id);
                                            final Request r = new Request("*", "GET", "/web", map, 2);
                                            Main.imp().getWebServer().getRequestManager().addToken(r);
                                        }
                                    }
                                });
                                Web.this.timestamps.put(player.getName(), System.currentTimeMillis());
                            }
                        });
                        return true;
                    }
                }
                return true;
            }
            case "upload": {
                if (!Permissions.hasPermission(player, "plots.web.upload")) {
                    MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.upload");
                    return false;
                }
                if (args.length < 2) {
                    MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot web upload <schematic|world>");
                    return false;
                }
                switch (args[1].toLowerCase()) {
                    case "schematic": {
                        if (!Permissions.hasPermission(player, "plots.web.upload.schematic")) {
                            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.upload.schematic");
                            return false;
                        }
                        final Plot plot = player.getCurrentPlot();
                        if ((plot == null) || !plot.isAdded(player.getUUID()) && !Permissions.hasPermission(player, "plots.web.download.other")) {
                            MainUtil.sendMessage(player, C.NO_PLOT_PERMS);
                            return true;
                        }
                        MainUtil.sendMessage(player, "&6Generating link...");
                        final String id = WebResource.nextId();
                        final String port;
                        if (Main.config().PORT != 80) {
                            port = ":" + Main.config().PORT;
                        } else {
                            port = "";
                        }
                        WebResource.uploads.put(id, plot);
                        MainUtil.sendMessage(player, "Upload the schematic: " + Main.config().WEB_IP + port + "/web?id=" + id);
                        final HashMap<String, String> map = new HashMap<>();
                        map.put("id", id);
                        final Request r = new Request("*", "*", "/web", map, 2);
                        Main.imp().getWebServer().getRequestManager().addToken(r);
                        return false;
                    }
                    case "world": {
                        if (!Permissions.hasPermission(player, "plots.web.upload.world")) {
                            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.upload.world");
                            return false;
                        }
                        PlotArea area = PS.get().getPlotArea("*", null);
                        if (area == null) {
                            MainUtil.sendMessage(player, "&6World uploads are disabled!");
                            return false;
                        }
                        MainUtil.sendMessage(player, "&6Generating link...");
                        final String id = WebResource.nextId();
                        final String port;
                        if (Main.config().PORT != 80) {
                            port = ":" + Main.config().PORT;
                        } else {
                            port = "";
                        }
                        WebResource.worldUploads.put(id, player);
                        MainUtil.sendMessage(player, "Upload the world: " + Main.config().WEB_IP + port + "/web?id=" + id);
                        final HashMap<String, String> map = new HashMap<>();
                        map.put("id", id);
                        final Request r = new Request("*", "*", "/web", map, 2000);
                        Main.imp().getWebServer().getRequestManager().addToken(r);
                        return false;
                    }
                }
            }
            default: {
                noargs(player);
                return false;
            }
        }
    }

}
