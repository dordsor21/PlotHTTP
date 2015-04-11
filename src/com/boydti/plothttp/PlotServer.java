package com.boydti.plothttp;

import java.util.Map;
import java.util.Map.Entry;

import com.boydti.plothttp.NanoHTTPD.Response.Status;

public class PlotServer extends NanoHTTPD {
    public PlotServer() {
        super(Main.port);
    }
    
    @Override public Response serve(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String ip = headers.get("remote-addr");
        
        if (Main.whitelist) {
            if (!Main.allowedIps.contains(ip)) {
                System.out.print("Denied query from: " + ip);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                return new NanoHTTPD.Response(Status.FORBIDDEN, MIME_PLAINTEXT, "403 FORBIDDEN");
            }
        }
        
        
        Method method = session.getMethod();
        String uri = session.getUri();
        Map<String, String> params = session.getParms();
        
        System.out.print("IP: " + ip);
        System.out.print("METHOD: " + method.name());
        System.out.print("URI: " + uri);
        System.out.print("PARAMS:");
        for (Entry<String, String> entry : params.entrySet()) {
            System.out.print(" - " + entry.getKey() + "=" + entry.getValue());
        }
        
        
        // TODO check if serving API
        
        /*
         * METHOD GET:
         * '/plot'
         * '/plots'
         * 
         * ETC
         * 
         *  
         * 
         */
        
        // TODO If not valid API call check if serving content
        
        // The result
        String msg = "";
        
        return new NanoHTTPD.Response(msg);
    }
}
