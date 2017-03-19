package com.boydti.plothttp.object;

import com.boydti.fawe.util.TaskManager;
import com.boydti.plothttp.Main;
import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.boydti.plothttp.util.NanoHTTPD.Response;
import com.boydti.plothttp.util.WebUtil;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.Auto;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.SchematicHandler;
import com.intellectualcrafters.plot.util.SchematicHandler.Dimension;
import com.intellectualcrafters.plot.util.SchematicHandler.Schematic;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.Bukkit;

public class WebResource extends Resource {

    // TODO this will be an interactive webpage
    private static SecureRandom random = new SecureRandom();

    // File pointers
    public static HashMap<String, String> downloads = new HashMap<>();

    // Plot pointers
    public static HashMap<String, Plot> uploads = new HashMap<>();
    public static HashMap<String, PlotPlayer> worldUploads = new HashMap<>();

    public static String nextId() {
        return new BigInteger(130, random).toString(32);
    }

    private static boolean isDownload = false;
    private static String filename = "plots.schematic";

    @Override
    public String toString() {
        return "web";
    }

    // will return an HTML web page
    @Override
    public byte[] getResult(final Request request, final IHTTPSession session) {
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
                    file = new File(Main.imp().getDataFolder() + File.separator + "downloads" + File.separator + download);
                    if (!file.exists()) {
                        return null;
                    }
                    isDownload = true;
                    final String name = file.getName();
                    final String[] split = name.split(",");
                    WebResource.filename = Main.config().CONTENT.FILENAME.replaceAll("%id%", split[0]).replaceAll("%world%", split[1]).replaceAll("%player%", split[2].split("\\.")[0]);
                    WebResource.filename = WebResource.filename.replaceAll("[\\W]|_", "-");
                    if (file.getName().endsWith(".zip")) {
                        WebResource.filename += ".zip";
                    } else {
                        WebResource.filename += ".schematic";
                    }
                }
                try {
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
                        final String filename = upload.getId().x + ";" + upload.getId().y + "," + upload.getArea() + ".schematic";
                        final String directory = Main.imp().getDataFolder() + File.separator + "uploads" + File.separator + filename;

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
                        final Schematic schem = SchematicHandler.manager.getSchematic(new File(directory));
                        if (schem == null) {
                            return "Invalid schematic file".getBytes();
                        }
                        final int length2 = upload.getTop().getX() - upload.getBottom().getX() + 1;
                        final Dimension dem = schem.getSchematicDimension();
                        if ((dem.getX() > length2) || (dem.getZ() > length2)) {
                            return "Invalid dimensions".getBytes();
                        }
                        final Thread thread = Thread.currentThread();
                        SchematicHandler.manager.paste(schem, upload, 0, 0, 0, true, new RunnableVal<Boolean>() {
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
                if (player != null) {
                    if (session.getMethod().name().equals("POST")) {
                        Main.imp().getWebServer().getRequestManager().removeToken(request);
                        worldUploads.remove(id);
                        if (player.isOnline()) {
                            try {
                                PlotArea area = PS.get().getPlotArea("*", null);
                                if (area != null) {
                                    Plot plot = TaskManager.IMP.sync(new com.boydti.fawe.object.RunnableVal<Plot>() {
                                        @Override
                                        public void run(Plot o) {
                                            int currentPlots = Settings.Limit.GLOBAL ? player.getPlotCount() : player.getPlotCount(area.worldname);
                                            int diff = player.getAllowedPlots() - currentPlots;
                                            if (diff < 1) {
                                                MainUtil.sendMessage(player, C.CANT_CLAIM_MORE_PLOTS_NUM, -diff + "");
                                                return;
                                            }
                                            if (area.getMeta("lastPlot") == null) {
                                                area.setMeta("lastPlot", new PlotId(0, 0));
                                            }
                                            PlotId lastId = (PlotId) area.getMeta("lastPlot");
                                            while (true) {
                                                lastId = Auto.getNextPlotId(lastId, 1);
                                                if (area.canClaim(player, lastId, lastId)) {
                                                    break;
                                                }
                                            }
                                            area.setMeta("lastPlot", lastId);
                                            this.value = area.getPlot(lastId);
                                            this.value.setOwner(player.getUUID());
                                        }
                                    });
                                    if (plot != null) {
                                        final Map<String, String> files = new HashMap<String, String>();
                                        session.parseBody(files);
                                        PlotId pid = plot.getId();
                                        File directory = new File(Bukkit.getWorldContainer(), pid.x + "," + pid.y + File.separator + "region");
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
                                                Files.copy(tempfile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                            } else if (key.endsWith(".zip")) {
                                                FileInputStream fis = new FileInputStream(tempfile);
                                                ZipInputStream zis = new ZipInputStream(new LimitedSizeInputStream(new BufferedInputStream(fis), Main.config().CONTENT.MAX_UPLOAD));
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
                                                        FileOutputStream fos = new FileOutputStream(new File(directory, name));
                                                        BufferedOutputStream dest = new BufferedOutputStream(fos, buffer);
                                                        while (total + buffer <= Main.config().CONTENT.MAX_UPLOAD && (count = zis.read(data, 0, buffer)) != -1) {
                                                            dest.write(data, 0, count);
                                                            total += count;
                                                        }
                                                        dest.flush();
                                                        dest.close();
                                                        zis.closeEntry();
                                                        entries++;
                                                        if (entries > max_entries) {
                                                            player.sendMessage("Too many files to unzip. (1024)");
                                                            throw new IllegalStateException("Too many files to unzip.");
                                                        }
                                                        if (total > Main.config().CONTENT.MAX_UPLOAD) {
                                                            player.sendMessage("File being unzipped is too big." + plot);
                                                            throw new IllegalStateException("File being unzipped is too big.");
                                                        }
                                                    }
                                                } finally {
                                                    zis.close();
                                                }
                                            } else {
                                                player.sendMessage("Skipping non world file: " + key);
                                            }
                                        }
                                        result.append("Your new world is located at " + plot);
                                        if (player.isOnline()) {
                                            TaskManager.IMP.sync(new com.boydti.fawe.object.RunnableVal<Object>() {
                                                @Override
                                                public void run(Object o) {
                                                    plot.teleportPlayer(player);
                                                }
                                            });
                                        }
                                        player.sendMessage("Your new world is located at " + plot);
                                    } else {
                                        MainUtil.sendMessage(player, C.NO_FREE_PLOTS);
                                        result.append(C.NO_FREE_PLOTS.s());
                                    }
                                } else {
                                    player.sendMessage("Server does not allow world uploads!");
                                    result.append("Server does not allow world uploads!");
                                }
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            player.sendMessage("You must be online to upload a world!");
                            result.append("You must be online to upload a world!");
                        }
                    } else {
                        String links = "";
                        for (final String link : Main.config().CONTENT.LINKS) {
                            links += "<div id=button>" + link + "</div>";
                        }
                        result.append(WebUtil.getPage("uploadworld").replaceAll("%links%", links));
                    }
                    return result.toString().getBytes();
                }
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
            isDownload = false;
        }
    }
}
