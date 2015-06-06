package com.boydti.plothttp.command;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.WebResource;
import com.boydti.plothttp.util.RequestManager;
import com.boydti.plothttp.util.WorldUtil;
import com.intellectualcrafters.jnbt.CompoundTag;
import com.intellectualcrafters.plot.commands.SubCommand;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.SchematicHandler;
import com.intellectualcrafters.plot.util.TaskManager;
import com.intellectualcrafters.plot.util.bukkit.UUIDHandler;

public class Web extends SubCommand {

    public Web() {
        super("web", "plots.web", "Web related commands", "", "web", CommandCategory.DEBUG, false);
    }
    
    public void noargs(PlotPlayer player) {
        ArrayList<String> args = new ArrayList<String>();
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
                if (args.length != 2 || (!args[1].equalsIgnoreCase("schematic") && !args[1].equalsIgnoreCase("world"))) {
                    MainUtil.sendMessage(player, C.COMMAND_SYNTAX, "/plot download <schematic|world>");
                    return false;
                }
                final Plot plot = MainUtil.getPlot(player.getLocation());
                if (plot == null || !plot.isAdded(player.getUUID())) {
                    MainUtil.sendMessage(player, C.NO_PLOT_PERMS);
                    return true;
                }
                final String id = WebResource.nextId();
                final String port;
                if (Main.port != 80) {
                    port = ":" + Main.port;
                }
                else {
                    port = "";
                }
                MainUtil.sendMessage(player, "&6Please wait while we process your plot...");
                switch (args[1].toLowerCase()) {
                    case "world": {
                        if (!Permissions.hasPermission(player, "plots.web.download.world")) {
                            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.download.world");
                            return false;
                        }
                        final String owner = UUIDHandler.getName(plot.owner);
                        if (owner == null) {
                            MainUtil.sendMessage(player, "&7(invalid owner) Could not export &c" + plot.id);
                        }
                        String filename = plot.id.x + ";" + plot.id.y + "," + plot.world + "," + owner + ".zip";
                        Bukkit.getWorld(plot.world).save();
                        boolean result;
                        try {
                            result = WorldUtil.save(plot, filename);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            result = false;
                        }
                        if (result) {
                            MainUtil.sendMessage(null, "&7 - &a  success: " + plot.id);
                            WebResource.downloads.put(id, filename);
                            MainUtil.sendMessage(player, "Download the file:\n" + Main.ip + port + "/web?id=" + id);
                            HashMap<String, String> map = new HashMap<>();
                            map.put("id", id);
                            Request r = new Request("*", "GET", "/web", map, 1);
                            RequestManager.addToken(r);
                            return true;
                        }
                        else {
                            MainUtil.sendMessage(player, "&7Could not export &c" + plot.id);
                            return false;
                        }
                    }
                    case "schematic": {
                        if (!Permissions.hasPermission(player, "plots.web.download.schematic")) {
                            MainUtil.sendMessage(player, C.NO_PERMISSION, "plots.web.download.schematic");
                            return false;
                        }
                        final CompoundTag sch = SchematicHandler.manager.getCompoundTag(plot.world, plot.id);
                        final String o = UUIDHandler.getName(plot.owner);
                        final String owner = o == null ? "unknown" : o;
                        if (sch == null) {
                            MainUtil.sendMessage(player, "&7Could not export &c" + plot.id);
                            return false;
                        }
                        TaskManager.runTaskAsync(new Runnable() {
                            @Override
                            public void run() {
                                MainUtil.sendMessage(player, "&6Generating link...");
                                String filename = plot.id.x + ";" + plot.id.y + "," + plot.world + "," + owner + ".schematic";
                                final boolean result = SchematicHandler.manager.save(sch, Main.plugin.getDataFolder() + File.separator + "downloads" + File.separator + filename);
                                if (!result) {
                                    MainUtil.sendMessage(player, "&7Could not export &c" + plot.id);
                                } else {
                                    MainUtil.sendMessage(null, "&7 - &a  success: " + plot.id);
                                    WebResource.downloads.put(id, filename);
                                    MainUtil.sendMessage(player, "Download the file:\n" + Main.ip + port + "/web?id=" + id);
                                    HashMap<String, String> map = new HashMap<>();
                                    map.put("id", id);
                                    Request r = new Request("*", "GET", "/web", map, 1);
                                    RequestManager.addToken(r);
                                }
                            }
                        });
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
                if (plot == null || !plot.isAdded(player.getUUID())) {
                    MainUtil.sendMessage(player, C.NO_PLOT_PERMS);
                    return true;
                }
                MainUtil.sendMessage(player, "&6Generating link...");
                final String id = WebResource.nextId();
                final String port;
                if (Main.port != 80) {
                    port = ":" + Main.port;
                }
                else {
                    port = "";
                }
                WebResource.uploads.put(id, plot);
                MainUtil.sendMessage(player, "Upload the file:\n" + Main.ip + port + "/web?id=" + id);
                HashMap<String, String> map = new HashMap<>();
                map.put("id", id);
                Request r = new Request("*", "*", "/web", map, 1);
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
