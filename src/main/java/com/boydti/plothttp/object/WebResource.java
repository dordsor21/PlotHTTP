package com.boydti.plothttp.object;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.boydti.plothttp.util.NanoHTTPD.Response;
import com.boydti.plothttp.util.WebUtil;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.adventure.text.minimessage.Template;
import com.plotsquared.core.configuration.caption.LocaleHolder;
import com.plotsquared.core.configuration.caption.StaticCaption;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.schematic.Schematic;
import com.plotsquared.core.services.plots.AutoService;
import com.plotsquared.core.util.SchematicHandler;
import com.plotsquared.core.util.task.RunnableVal;
import com.plotsquared.core.util.task.TaskManager;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WebResource extends Resource {

    // File pointers
    public static HashMap<String, String> downloads = new HashMap<>();
    public static HashMap<String, UUID> downloadsUUID = new HashMap<>();
    // Plot pointers
    public static HashMap<String, Plot> uploads = new HashMap<>();
    public static HashMap<String, PlotPlayer> worldUploads = new HashMap<>();
    // TODO this will be an interactive webpage
    private static SecureRandom random = new SecureRandom();
    private static boolean isDownload = false;
    private static String filename = "plots.schematic";

    public static String nextId() {
        return new BigInteger(130, random).toString(32);
    }

    @Override
    public String toString() {
        return "web";
    }

    // will return an HTML web page
    @Override
    public byte[] getResult(final Request request, final IHTTPSession session) {
        isDownload = false;
        String page = request.ARGS.get("page");
        if (page == null) {
            page = "index";
        }
        final StringBuilder result = new StringBuilder();
        final String id = request.ARGS.get("id");
        if (id != null) {
            // downloads
            final String download = downloads.get(id);
            if (download != null) {
                UUID uuid = downloadsUUID.get(id);
                final File tmp = new File(download);
                File file;
                if (tmp.isAbsolute()) {
                    file = tmp;
                    if (!file.exists()) {
                        return null;
                    }
                    isDownload = true;
                    WebResource.filename = file.getName();
                } else {
                    file = new File(Main.imp().DIR + File.separator + "downloads" + File.separator + download);
                    if (!file.exists()) {
                        return null;
                    }
                    isDownload = true;
                    final String name = file.getName();
                    final String[] split = name.split(",");
                    WebResource.filename = Main.config().CONTENT.FILENAME.replaceAll("%id%", split[0]).replaceAll(
                            "%world%",
                            split[1]
                    ).replaceAll("%player%", split[2].split("\\.")[0]);
                    WebResource.filename = WebResource.filename.replaceAll("[\\W]|_", "-");
                    if (file.getName().endsWith(".zip")) {
                        WebResource.filename += ".zip";
                    } else {
                        WebResource.filename += ".schematic";
                    }
                }
                try {
                    Main.imp().getLogger().logFileRequest(uuid, file, request, session);
                    return Files.readAllBytes(file.toPath());
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }

            // uploads (TODO)
            {
                final Plot upload = uploads.get(id);
                if (upload != null) {
                    if (session.getMethod().name().equals("POST")) {
                        uploads.remove(id);
                        Main.imp().getWebServer().getRequestManager().removeToken(request);
                        final String filename = upload.getId().getX() + ";" + upload.getId().getY() + "," + upload.getArea() +
                                ".schematic";
                        final String directory = Main.imp().DIR + File.separator + "uploads" + File.separator + filename;

                        try {
                            final Map<String, String> files = new HashMap<String, String>();
                            session.parseBody(files);
                            final Set<String> keys = files.keySet();
                            for (final String key : keys) {
                                final String location = files.get(key);
                                final File tempfile = new File(location);
                                Files.copy(tempfile.toPath(), new File(directory).toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        final Schematic schem;
                        try {
                            schem = new PlotAPI().getSchematicHandler().getSchematic(new File(directory));
                        } catch (SchematicHandler.UnsupportedFormatException e) {
                            e.printStackTrace();
                            return e.getMessage().getBytes();
                        }
                        if (schem == null) {
                            return "Invalid schematic file".getBytes();
                        }
                        final int length2 = upload.getTop().getX() - upload.getBottom().getX() + 1;
                        final BlockVector3 dem = schem.getClipboard().getDimensions();
                        if ((dem.getX() > length2) || (dem.getZ() > length2)) {
                            return "Invalid dimensions".getBytes();
                        }
                        final Thread thread = Thread.currentThread();
                        SchematicHandler.manager.paste(schem, upload, 0, 0, 0, true, null, new RunnableVal<>() {
                            @Override
                            public void run(Boolean value) {
                                if (value) {
                                    result.append("Success!");
                                } else {
                                    result.append("An error occured (see console for more information)!");
                                }
                                thread.notify();
                            }
                        });
                        try {
                            thread.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    } else {
                        String links = "";
                        for (final String link : Main.config().CONTENT.LINKS) {
                            links += "<div id=button>" + link + "</div>";
                        }

                        result.append(WebUtil.getPage("upload").replaceAll("%links%", links));
                    }
                    return result.toString().getBytes();
                }
            }
            // Invalid / Expired?
            {
                PlotPlayer player = worldUploads.get(id);
                if (player == null) {
                    result.append(WebUtil.getPage(page));
                    return result.toString().getBytes();
                }
                if (session.getMethod().name().equals("POST")) {
                    Main.imp().getWebServer().getRequestManager().removeToken(request);
                    worldUploads.remove(id);
                    if (!((Player) player.getPlatformPlayer()).isOnline()) {
                        player.sendMessage(StaticCaption.of("You must be online to upload a world!"));
                        result.append("You must be online to upload a world!");
                        return result.toString().getBytes();
                    }
                    try {
                        PlotArea area = PlotSquared.get().getPlotAreaManager().getPlotArea("*", null);
                        if (area == null) {
                            player.sendMessage(StaticCaption.of("Server does not allow world uploads!"));
                            result.append("Server does not allow world uploads!");
                            return result.toString().getBytes();
                        }
                        final Map<String, String> files = new HashMap<>();
                        session.parseBody(files);
                        TaskManager.getPlatformImplementation().sync(() -> {
                            int currentPlots = Settings.Limit.GLOBAL
                                    ? player.getPlotCount()
                                    : player.getPlotCount(area.getWorldName());
                            int diff = player.getAllowedPlots() - currentPlots;
                            if (diff < 1) {
                                player.sendMessage(
                                        TranslatableCaption.of("permission.cant_claim_more_plots"),
                                        Template.of("amount", String.valueOf(player.getAllowedPlots()))
                                );
                                return null;
                            }

                            Plot plot =
                                    new AutoService.SinglePlotService().handle(new AutoService.AutoQuery(
                                            player,
                                            null,
                                            1,
                                            1,
                                            area
                                    )).get(0);
                            if (plot == null) {
                                return null;
                            }
                            try {
                                PlotId pid = plot.getId();
                                File directory = new File(
                                        Bukkit.getWorldContainer(),
                                        pid.getX() + "," + pid.getY() + File.separator + "region"
                                );
                                if (!directory.exists()) {
                                    directory.mkdirs();
                                }
                                final Set<String> keys = files.keySet();
                                for (final String key : keys) {
                                    final String location = files.get(key);
                                    final File tempfile = new File(location);
                                    if (key.endsWith(".mca")) {
                                        File newFile = new File(directory, key);
                                        if (!newFile.exists()) {
                                            System.out.println(newFile.getParentFile().mkdirs());
                                        }
                                        Files.copy(
                                                tempfile.toPath(),
                                                newFile.toPath(),
                                                StandardCopyOption.REPLACE_EXISTING
                                        );
                                    } else if (key.endsWith(".zip")) {
                                        FileInputStream fis = new FileInputStream(tempfile);
                                        ZipInputStream zis = new ZipInputStream(new LimitedSizeInputStream(
                                                new BufferedInputStream(fis),
                                                Main.config().CONTENT.MAX_UPLOAD
                                        ));
                                        ZipEntry entry;
                                        int entries = 0;
                                        long total = 0;
                                        int buffer = 4096;
                                        int max_entries = 1024;
                                        try {
                                            while ((entry = zis.getNextEntry()) != null) {
                                                int count;
                                                byte data[] = new byte[buffer];
                                                // Write the files to the disk, but ensure that the filename is valid,
                                                // and that the file is not insanely big
                                                String name = entry.getName();
                                                if (!name.endsWith(".mca")) {
                                                    continue;
                                                }
                                                String[] split = name.split("[\\|/]");
                                                name = split[split.length - 1];
                                                FileOutputStream fos = new FileOutputStream(new File(
                                                        directory,
                                                        name
                                                ));
                                                BufferedOutputStream dest = new BufferedOutputStream(
                                                        fos,
                                                        buffer
                                                );
                                                while (total + buffer <= Main.config().CONTENT.MAX_UPLOAD && (count = zis.read(
                                                        data,
                                                        0,
                                                        buffer
                                                )) != -1) {
                                                    dest.write(data, 0, count);
                                                    total += count;
                                                }
                                                dest.flush();
                                                dest.close();
                                                zis.closeEntry();
                                                entries++;
                                                if (entries > max_entries) {
                                                    player.sendMessage(StaticCaption.of("Too many files to unzip. (1024)"));
                                                    throw new IllegalStateException(
                                                            "Too many files to unzip.");
                                                }
                                                if (total > Main.config().CONTENT.MAX_UPLOAD) {
                                                    player.sendMessage(StaticCaption.of("File being unzipped is too big." + plot));
                                                    throw new IllegalStateException(
                                                            "File being unzipped is too big.");
                                                }
                                            }
                                        } finally {
                                            zis.close();
                                        }
                                    } else {
                                        player.sendMessage(StaticCaption.of("Skipping non world file: " + key));
                                    }
                                }
                                result.append("Your new world is located at " + plot);
                                plot.claim(player, false, null, false);
                                if (!((Player) player.getPlatformPlayer()).isOnline()) {
                                    plot.teleportPlayer(player, null);
                                }
                                player.sendMessage(StaticCaption.of("Your new world is located at " + plot));
                                return null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            player.sendMessage(TranslatableCaption.of("errors.no_free_plots"));
                            result.append(TranslatableCaption
                                    .of("errors.no_free_plots")
                                    .getComponent(LocaleHolder.console()));
                            return null;
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    StringBuilder links = new StringBuilder();
                    for (final String link : Main.config().CONTENT.LINKS) {
                        links.append("<div id=button>").append(link).append("</div>");
                    }
                    result.append(WebUtil.getPage("uploadworld").replaceAll("%links%", links.toString()));
                }
                return result.toString().getBytes();
            }
        }


        result.append(WebUtil.getPage(page));
        return result.toString().getBytes();
    }

    @Override
    public void process(final Response page) {
        if (isDownload) {
            page.addHeader("Content-Disposition", "attachment; filename=" + filename);
            filename = "plot.schematic";
        }
    }

    @Override
    public void onSuccess(Response page) {
        if (isDownload) {
            Main.imp().getLogger().writeLine("All bytes sent!\n");
        }
    }

}
