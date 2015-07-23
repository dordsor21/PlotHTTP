package com.boydti.plothttp.command;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.WebResource;
import com.boydti.plothttp.util.RequestManager;
import com.boydti.plothttp.util.WorldUtil;
import com.intellectualcrafters.jnbt.CompoundTag;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.SchematicHandler;
import com.intellectualcrafters.plot.util.TaskManager;
import com.intellectualcrafters.plot.util.bukkit.UUIDHandler;

public class Web extends SubCommand {

    public Web() {
        super("web", "plots.web", "Web related commands", "", "web", CommandCategory.DEBUG, false);
    }

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
            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.<reload|download|upload>");
            return;
        }
        MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot web <" + StringUtils.join(args, "|") + ">");
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
    public boolean execute(final PlotPlayer player, final String... args) {
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
                Main.plugin.onDisable();
                Main.plugin.onEnable();
                MainUtil.sendMessage(player, "&aReloaded success!");
                return true;
            }
            case "download": {
                if (!Permissions.hasPermission(player, "plots.web.download")) {
                    MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.download");
                    return false;
                }
                if (args.length < 2) {
                    MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot download <schematic|world|worldedit>");
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
                        plot = MainUtil.getPlot(player.getLocation());
                        if ((plot == null) || (!plot.isAdded(player.getUUID()) && !Permissions.hasPermission(player, "plots.web.download.other"))) {
                            MainUtil.sendMessage(player, C.NO_PLOT_PERMS);
                            return false;
                        }
                        break;
                    }
                    default: {
                        MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot download <schematic|world|worldedit>");
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
                if (Main.port != 80) {
                    port = ":" + Main.port;
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
                                MainUtil.sendMessage(player, "Download the file: " + Main.ip + port + "/web?id=" + id);
                                final HashMap<String, String> map = new HashMap<>();
                                map.put("id", id);
                                final Request r = new Request("*", "GET", "/web", map, 1);
                                RequestManager.addToken(r);
                            }
                        });
                        this.timestamps.put(player.getName(), System.currentTimeMillis());
                        return true;
                    }
                    case "world": {
                        final String owner = UUIDHandler.getName(plot.owner);
                        if (owner == null) {
                            MainUtil.sendMessage(player, "&7(invalid owner) Could not export &c" + plot.id);
                        }
                        final String filename = plot.id.x + ";" + plot.id.y + "," + plot.world + "," + owner + ".zip";
                        final World world = Bukkit.getWorld(plot.world);
                        world.save();
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
                            TaskManager.runTaskLater(new Runnable() {
                                @Override
                                public void run() {
                                    MainUtil.sendMessage(player, "Download the file: " + Main.ip + port + "/web?id=" + id);
                                    final HashMap<String, String> map = new HashMap<>();
                                    map.put("id", id);
                                    final Request r = new Request("*", "GET", "/web", map, 1);
                                    RequestManager.addToken(r);
                                }
                            }, 200);
                            this.timestamps.put(player.getName(), System.currentTimeMillis());
                            return true;
                        } else {
                            MainUtil.sendMessage(player, "&7Could not export &c" + plot.id);
                            return false;
                        }
                    }
                    case "schematic": {
                        MainUtil.sendMessage(player, "&6Processing plot...");
                        SchematicHandler.manager.getCompoundTag(plot.world, plot.id, new RunnableVal<CompoundTag>() {
                            @Override
                            public void run() {
                                final String o = UUIDHandler.getName(plot.owner);
                                final String owner = o == null ? "unknown" : o;
                                if (value == null) {
                                    MainUtil.sendMessage(player, "&7Could not export &c" + plot.id);
                                    return;
                                }
                                TaskManager.runTaskAsync(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainUtil.sendMessage(player, "&6Generating link...");
                                        final String filename = plot.id.x + ";" + plot.id.y + "," + plot.world + "," + owner + ".schematic";
                                        final boolean result = SchematicHandler.manager.save(value, Main.plugin.getDataFolder() + File.separator + "downloads" + File.separator + filename);
                                        if (!result) {
                                            MainUtil.sendMessage(player, "&7Could not export &c" + plot.id);
                                        } else {
                                            MainUtil.sendMessage(null, "&7 - &a  success: " + plot.id);
                                            WebResource.downloads.put(id, filename);
                                            MainUtil.sendMessage(player, "Download the file: " + Main.ip + port + "/web?id=" + id);
                                            final HashMap<String, String> map = new HashMap<>();
                                            map.put("id", id);
                                            final Request r = new Request("*", "GET", "/web", map, 1);
                                            RequestManager.addToken(r);
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
                    MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.reload");
                    return false;
                }
                final Plot plot = MainUtil.getPlot(player.getLocation());
                if ((plot == null) || !plot.isAdded(player.getUUID()) && !Permissions.hasPermission(player, "plots.web.download.other")) {
                    MainUtil.sendMessage(player, C.NO_PLOT_PERMS);
                    return true;
                }
                MainUtil.sendMessage(player, "&6Generating link...");
                final String id = WebResource.nextId();
                final String port;
                if (Main.port != 80) {
                    port = ":" + Main.port;
                } else {
                    port = "";
                }
                WebResource.uploads.put(id, plot);
                MainUtil.sendMessage(player, "Upload the file: " + Main.ip + port + "/web?id=" + id);
                final HashMap<String, String> map = new HashMap<>();
                map.put("id", id);
                final Request r = new Request("*", "*", "/web", map, 1);
                RequestManager.addToken(r);
                return false;
            }
            default: {
                noargs(player);
                return false;
            }
        }
    }

}
