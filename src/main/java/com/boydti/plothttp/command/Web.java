package com.boydti.plothttp.command;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.WebResource;
import com.boydti.plothttp.util.WorldUtil;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.command.CommandCategory;
import com.plotsquared.core.command.CommandDeclaration;
import com.plotsquared.core.command.RequiredType;
import com.plotsquared.core.command.SubCommand;
import com.plotsquared.core.configuration.caption.StaticCaption;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.configuration.file.YamlConfiguration;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.world.SinglePlotArea;
import com.plotsquared.core.util.Permissions;
import com.plotsquared.core.util.StringMan;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.util.task.TaskTime;
import com.plotsquared.core.configuration.adventure.text.minimessage.Template;
import org.bukkit.entity.Player;

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

    private final HashMap<String, Long> timestamps = new HashMap<>();

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
            player.sendMessage(
                    TranslatableCaption.of("permission.no_permission"),
                    Template.of("node", "plots.web.reload")
            );
            return;
        }
        player.sendMessage(
                TranslatableCaption.of("permission.no_permission"),
                Template.of("node", "plots.web.reload")
        );
        player.sendMessage(
                TranslatableCaption.of("commandconfig.command_syntax"),
                Template.of("value", "/plot web <" + StringMan.join(args, "|") + ">")
        );
    }

    @Override
    public boolean onCommand(final PlotPlayer player, final String... args) {
        if (args.length == 0) {
            noargs(player);
            return false;
        }
        switch (args[0]) {
            case "reload": {
                if (!Permissions.hasPermission(player, "plots.web.reload")) {
                    player.sendMessage(
                            TranslatableCaption.of("permission.no_permission"),
                            Template.of("node", "plots.web.reload")
                    );
                    return false;
                }
                Main.imp().close();
                Main.imp().open();
                player.sendMessage(StaticCaption.of("Reload success!"));
                return true;
            }
            case "download": {
                if (!Permissions.hasPermission(player, "plots.web.download")) {
                    player.sendMessage(
                            TranslatableCaption.of("permission.no_permission"),
                            Template.of("node", "plots.web.download")
                    );
                    return false;
                }
                if (args.length < 2) {
                    player.sendMessage(
                            TranslatableCaption.of("commandconfig.command_syntax"),
                            Template.of("value", "/plot web download <schematic|world|worldedit>")
                    );
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
                        if ((plot == null) || (!plot.isAdded(player.getUUID()) && !Permissions.hasPermission(
                                player,
                                "plots.web.download.other"
                        ))) {
                            player.sendMessage(
                                    TranslatableCaption.of("permission.no_plot_perms")
                            );
                            return false;
                        }
                        break;
                    }
                    default: {
                        player.sendMessage(
                                TranslatableCaption.of("commandconfig.command_syntax"),
                                Template.of("value", "/plot web download <schematic|world|worldedit>")
                        );
                        return false;
                    }
                }
                final Long last = this.timestamps.get(player.getName());
                if (last != null) {
                    if (last < 600000) {
                        final int left = (int) ((System.currentTimeMillis() - last) / 60000);
                        player.sendMessage(StaticCaption.of("You must wait " + left + " minutes before another download!"));
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
                    player.sendMessage(
                            TranslatableCaption.of("permission.no_permission"),
                            Template.of("node", "plots.web.download." + args[1].toLowerCase())
                    );
                    return false;
                }
                player.sendMessage(StaticCaption.of("Please wait while we process your plot..."));
                switch (args[1].toLowerCase()) {
                    case "worldedit": {
                        if (!Permissions.hasPermission(player, "plots.web.download.worldedit")) {
                            player.sendMessage(
                                    TranslatableCaption.of("permission.no_permission"),
                                    Template.of("node", "plots.web.download.worldedit")
                            );
                            return false;
                        }
                        final File worldeditDir = new File("plugins" + File.separator + "WorldEdit");
                        final File worldeditFile = new File(worldeditDir + File.separator + "config.yml");
                        if (!worldeditFile.exists()) {
                            player.sendMessage(StaticCaption.of("WorldEdit not found!"));
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
                                player.sendMessage(StaticCaption.of(file.getName()));
                            }
                            player.sendMessage(StaticCaption.of("Please specify a file: /plot web download worldedit <file>"));
                            return false;
                        }
                        final File schem = new File(directory, args[2]);
                        if (!schem.exists()) {
                            final List<File> files = getFiles(directory, new ArrayList<File>());
                            for (final File file : files) {
                                player.sendMessage(StaticCaption.of(file.getName()));
                            }
                            player.sendMessage(StaticCaption.of("Please specify a file: /plot web download worldedit <file>"));
                            return false;
                        }
                        TaskManager.runTaskAsync(() -> {
                            player.sendMessage(StaticCaption.of("Generating link..."));
                            WebResource.downloads.put(id, schem.getAbsolutePath());
                            WebResource.downloadsUUID.put(id, player.getUUID());
                            player.sendMessage(
                                    TranslatableCaption.of("web.generation_link_success_legacy_world"),
                                    Template.of("url", Main.config().WEB_IP + port + "/web?id" + "=" + id)
                            );
                            final HashMap<String, String> map = new HashMap<>();
                            map.put("id", id);
                            final Request r = new Request("*", "GET", "/web", map, 2);
                            Main.imp().getWebServer().getRequestManager().addToken(r);
                        });
                        this.timestamps.put(player.getName(), System.currentTimeMillis());
                        return true;
                    }
                    case "world": {
                        if (!Permissions.hasPermission(player, "plots.web.download.world")) {
                            player.sendMessage(
                                    TranslatableCaption.of("permission.no_permission"),
                                    Template.of("node", "plots.web.download.world")
                            );
                            return false;
                        }
                        PlotSquared.get().getImpromptuUUIDPipeline().getSingle(
                                plot.getOwner(),
                                (owner, throwable) -> {
                                    if (owner == null) {
                                        player.sendMessage(StaticCaption.of("(invalid owner) Could not export " + plot.getId()));
                                        return;
                                    }
                                    String worldName = (plot.getArea() instanceof SinglePlotArea)
                                            ? plot.getId().toString().replace(';', ',')
                                            : plot.getArea().getWorldName();
                                    final String filename = plot.getId().getX() + "," + plot
                                            .getId()
                                            .getY() + "," + worldName + "," + owner + ".zip";
                                    PlotSquared.platform().worldUtil().saveWorld(worldName);
                                    boolean result;
                                    try {
                                        result = WorldUtil.save(plot, filename);
                                    } catch (final Exception e) {
                                        e.printStackTrace();
                                        result = false;
                                    }
                                    if (result) {
                                        player.sendMessage(StaticCaption.of(
                                                "Generating link... (this will take up to 10 seconds)"));
                                        WebResource.downloads.put(id, filename);
                                        WebResource.downloadsUUID.put(id, player.getUUID());
                                        TaskManager.runTaskLater(() -> {
                                            player.sendMessage(
                                                    TranslatableCaption.of("web.generation_link_success_legacy_world"),
                                                    Template.of("url", Main.config().WEB_IP + port + "/web?id" + "=" + id)
                                            );
                                            final HashMap<String, String> map = new HashMap<>();
                                            map.put("id", id);
                                            final Request r = new Request("*", "GET", "/web", map, 2);
                                            Main.imp().getWebServer().getRequestManager().addToken(r);
                                        }, TaskTime.ms(200));
                                        Web.this.timestamps.put(player.getName(), System.currentTimeMillis());
                                    } else {
                                        player.sendMessage(StaticCaption.of("Could not export " + plot.getId()));
                                    }
                                }
                        );
                        return true;
                    }
                    case "schematic": {
                        if (!Permissions.hasPermission(player, "plots.web.download.schematic")) {
                            player.sendMessage(
                                    TranslatableCaption.of("permission.no_permission"),
                                    Template.of("node", "plots.web.download.schematic")
                            );
                            return false;
                        }
                        player.sendMessage(StaticCaption.of("Processing plot..."));
                        new PlotAPI().getSchematicHandler().getCompoundTag(plot).whenComplete((value, throwable) -> PlotSquared
                                .get()
                                .getImpromptuUUIDPipeline()
                                .getSingle(
                                        plot.getOwner(),
                                        (o, t) -> {
                                            final String owner = o == null ? "unknown" : o;
                                            if (value == null) {
                                                player.sendMessage(StaticCaption.of("Could not export " + plot.getId()));
                                                return;
                                            }
                                            TaskManager.runTaskAsync(() -> {
                                                player.sendMessage(StaticCaption.of("Generating link..."));
                                                final String filename = plot.getId().getX() + ";" + plot
                                                        .getId()
                                                        .getY() + "," + plot.getArea() + "," + owner + ".schematic";
                                                final boolean result = new PlotAPI().getSchematicHandler().save(
                                                        value,
                                                        Main.imp().DIR + File.separator + "downloads" + File.separator + filename
                                                );
                                                if (!result) {
                                                    player.sendMessage(StaticCaption.of("Could not export " + plot.getId()));
                                                } else {
                                                    player.sendMessage(StaticCaption.of("-  success: " + plot.getId()));
                                                    WebResource.downloads.put(id, filename);
                                                    WebResource.downloadsUUID.put(id, player.getUUID());
                                                    player.sendMessage(
                                                            TranslatableCaption.of("web.generation_link_success_legacy_world"),
                                                            Template.of(
                                                                    "url",
                                                                    Main.config().WEB_IP + port + "/web?id" + "=" + id
                                                            )
                                                    );
                                                    final HashMap<String, String> map = new HashMap<>();
                                                    map.put("id", id);
                                                    final Request r = new Request("*", "GET", "/web", map, 2);
                                                    Main.imp().getWebServer().getRequestManager().addToken(r);
                                                }
                                            });
                                            Web.this.timestamps.put(player.getName(), System.currentTimeMillis());
                                        }
                                ));
                        return true;
                    }
                }
                return true;
            }
            case "upload": {
                if (!Permissions.hasPermission(player, "plots.web.upload")) {
                    player.sendMessage(
                            TranslatableCaption.of("permission.no_permission"),
                            Template.of("node", "plots.web.upload")
                    );
                    return false;
                }
                if (args.length < 2) {
                    player.sendMessage(
                            TranslatableCaption.of("commandconfig.command_syntax"),
                            Template.of("value", "/plot web download <schematic|world>")
                    );
                    return false;
                }
                switch (args[1].toLowerCase()) {
                    case "schematic": {
                        if (!Permissions.hasPermission(player, "plots.web.upload.schematic")) {
                            player.sendMessage(
                                    TranslatableCaption.of("permission.no_permission"),
                                    Template.of("node", "plots.web.upload.schematic")
                            );
                            return false;
                        }
                        final Plot plot = player.getCurrentPlot();
                        if ((plot == null) || !plot.isAdded(player.getUUID()) && !Permissions.hasPermission(
                                player,
                                "plots.web.download.other"
                        )) {
                            player.sendMessage(
                                    TranslatableCaption.of("permission.no_plot_perms")
                            );
                            return true;
                        }
                        player.sendMessage(StaticCaption.of("Generating link..."));
                        final String id = WebResource.nextId();
                        final String port;
                        if (Main.config().PORT != 80) {
                            port = ":" + Main.config().PORT;
                        } else {
                            port = "";
                        }
                        WebResource.uploads.put(id, plot);
                        ((Player) player.getPlatformPlayer()).sendMessage("Upload the schematic: " + Main.config().WEB_IP + port + "/web" +
                                "?id=" + id);
                        final HashMap<String, String> map = new HashMap<>();
                        map.put("id", id);
                        final Request r = new Request("*", "*", "/web", map, 2);
                        Main.imp().getWebServer().getRequestManager().addToken(r);
                        return false;
                    }
                    case "world": {
                        if (!Permissions.hasPermission(player, "plots.web.upload.world")) {
                            player.sendMessage(
                                    TranslatableCaption.of("permission.no_permission"),
                                    Template.of("node", "plots.web.upload.world")
                            );
                            return false;
                        }
                        PlotArea area = PlotSquared.get().getPlotAreaManager().getPlotArea("*", null);
                        if (area == null) {
                            player.sendMessage(StaticCaption.of("World uploads are disabled!"));
                            return false;
                        }
                        player.sendMessage(StaticCaption.of("Generating link..."));
                        final String id = WebResource.nextId();
                        final String port;
                        if (Main.config().PORT != 80) {
                            port = ":" + Main.config().PORT;
                        } else {
                            port = "";
                        }
                        WebResource.worldUploads.put(id, player);
                        ((Player) player.getPlatformPlayer()).sendMessage("Upload the world: " + Main.config().WEB_IP + port + "/web?id=" + id);
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
