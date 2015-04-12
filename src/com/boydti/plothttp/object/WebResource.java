package com.boydti.plothttp.object;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.HashMap;

import com.boydti.plothttp.Main;
import com.boydti.plothttp.util.WebUtil;
import com.boydti.plothttp.util.NanoHTTPD.Response;

public class WebResource extends Resource {

    // TODO this will be an interactive webpage
    private static SecureRandom random = new SecureRandom();
    
    // File pointers
    public static HashMap<String, String> downloads = new HashMap<>();
    
    public static String nextId() {
        return new BigInteger(130, random).toString(32);
    }
    
    private static boolean isDownload = false;
    
    @Override
    public String toString() {
        return "web";
    }

    // will return an HTML web page
    @Override
    public byte[] getResult(Request request) {
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
                System.out.print(file.getPath());
                System.out.print(file.getAbsolutePath());
                if (file.exists()) {
                    isDownload = true;
                    try {
                        return Files.readAllBytes(file.toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            
            // uploads (TODO)
            
        }
        
        String result = WebUtil.getPage(page);
        return result.getBytes();
    }
    
    @Override
    public void process(Response page) {
        if (isDownload) {
            page.addHeader("Content-Disposition", "attachment; filename=plot.schematic");
            isDownload = false;
        }
    }
    
    
    
}
