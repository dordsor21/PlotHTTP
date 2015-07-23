package com.boydti.plothttp.object;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.boydti.plothttp.util.NanoHTTPD.Response;
import com.boydti.plothttp.util.WebUtil;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.RunnableVal;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.SchematicHandler;
import com.intellectualcrafters.plot.util.SchematicHandler.Dimension;
import com.intellectualcrafters.plot.util.SchematicHandler.Schematic;
import com.intellectualcrafters.plot.util.TaskManager;

public class WebResource extends Resource {

    // TODO this will be an interactive webpage
    private static SecureRandom random = new SecureRandom();

    // File pointers
    public static HashMap<String, String> downloads = new HashMap<>();

    // Plot pointers
    public static HashMap<String, Plot> uploads = new HashMap<>();

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
                    file = new File(Main.plugin.getDataFolder() + File.separator + "downloads" + File.separator + download);
                    if (!file.exists()) {
                        return null;
                    }
                    isDownload = true;
                    final String name = file.getName();
                    final String[] split = name.split(",");
                    WebResource.filename = Main.filename.replaceAll("%id%", split[0]).replaceAll("%world%", split[1]).replaceAll("%player%", split[2].split("\\.")[0]);
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

            final Plot upload = uploads.get(id);
            if (upload != null) {
                if (session.getMethod().name().equals("POST")) {
                    final String filename = upload.id.x + ";" + upload.id.y + "," + upload.world + ".schematic";
                    final String directory = Main.plugin.getDataFolder() + File.separator + "uploads" + File.separator + filename;

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
                    uploads.remove(id);
                    final Schematic schem = SchematicHandler.manager.getSchematic(new File(directory));
                    if (schem == null) {
                        return "Invalid schematic file".getBytes();
                    }
                    final int length2 = MainUtil.getPlotWidth(upload.world, upload.id);
                    final Dimension dem = schem.getSchematicDimension();
                    if ((dem.getX() > length2) || (dem.getZ() > length2)) {
                        return "Invalid dimensions".getBytes();
                    }
                    final Thread thread = Thread.currentThread();
                    SchematicHandler.manager.paste(schem, upload, 0, 0, new RunnableVal<Boolean>() {
                        @Override
                        public void run() {
                            if (value) {
                                result.append("Success!");
                            }
                            else {
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
                    for (final String link : Main.links) {
                        links += "<li>" + link + "</li>";
                    }

                    result.append(WebUtil.getPage("upload").replaceAll("%links%", "<ul>" + links + "</ul>"));
                }
                return result.toString().getBytes();
            }

            // Invalid / Expired?

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
