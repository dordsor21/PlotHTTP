package com.boydti.plothttp.object;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.util.WebUtil;
import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.boydti.plothttp.util.NanoHTTPD.Response;
import com.intellectualcrafters.plot.commands.SchematicCmd;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.Plot;
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
    public byte[] getResult(Request request, IHTTPSession session) {
        String page = request.ARGS.get("page");
        if (page == null) {
            page = "index";
        }
        String id = request.ARGS.get("id");
        if (id != null) {
            // downloads
            String download = downloads.get(id);
            if (download != null) {
                File file = new File(Main.plugin.getDataFolder() + File.separator + "downloads" + File.separator + download);
                if (file.exists()) {
                    isDownload = true;
                    String name = file.getName();
                    String[] split = name.split(",");
                    WebResource.filename = "";
                    WebResource.filename = Main.filename.replaceAll("%id%", split[0]).replaceAll("%world%", split[1]).replaceAll("%player%", split[2].split("\\.")[0]);
                    WebResource.filename = WebResource.filename.replaceAll("[\\W]|_", "-");
                    try {
                        return Files.readAllBytes(file.toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            // uploads (TODO)
            
            final Plot upload = uploads.get(id);
            if (upload != null) {
                String result;
                if (session.getMethod().name().equals("POST")) {
                    String filename = upload.id.x + ";" + upload.id.y + "," + upload.world + ".schematic";
                    String directory = Main.plugin.getDataFolder() + File.separator + "uploads" + File.separator + filename;
                    
                    try {
                        Map<String, String> files = new HashMap<String, String>();
                        session.parseBody(files);
                        Set<String> keys = files.keySet();
                        for(String key: keys){
                            String name = key;
                            String location = files.get(key);
                            File tempfile = new File(location);
                            Files.copy(tempfile.toPath(), new File(directory).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    uploads.remove(id);
                    final Schematic schem = SchematicHandler.manager.getSchematic(new File(directory));
                    
                    final int length2 = MainUtil.getPlotWidth(upload.world, upload.id);
                    final Dimension dem = schem.getSchematicDimension();
                    if ((dem.getX() > length2) || (dem.getZ() > length2)) {
                        return "Invalid dimensions".getBytes();
                    }
                    
                    TaskManager.runTask(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            SchematicHandler.manager.paste(schem, upload, 0, 0);
                        }
                    });
                    
                    result = "Success!";
                }
                else {
                    result = "<form method='POST' action='' enctype='multipart/form-data'>Upload file:<input type='file' name='file'/><input type='submit' value='Upload'/></form>";
                }
                return result.getBytes();
            }
            
            // Invalid / Expired?
            
        }
        
        String result = WebUtil.getPage(page);
        return result.getBytes();
    }
    
    @Override
    public void process(Response page) {
        if (isDownload) {
            page.addHeader("Content-Disposition", "attachment; filename=" + filename + ".schematic");
            filename = "plot";
            isDownload = false;
        }
    }
}
