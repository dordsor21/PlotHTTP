package com.boydti.plothttp;

import java.util.Map;
import java.util.Map.Entry;

import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.Resource;
import com.boydti.plothttp.util.NanoHTTPD;
import com.boydti.plothttp.util.RequestManager;
import com.boydti.plothttp.util.NanoHTTPD.Response.Status;
import com.boydti.plothttp.util.ResourceManager;

public class PlotServer extends NanoHTTPD {
    public PlotServer() {
        super(Main.port);
    }
    
    @Override public Response serve(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String ip = headers.get("remote-addr");
        Method method = session.getMethod();
        String uri = session.getUri();
        Map<String, String> args = session.getParms();


        //////////////////////////////// DEBUG STUFF ////////////////////////////////
        System.out.print("IP: " + ip);
        System.out.print("METHOD: " + method.name());
        System.out.print("URI: " + uri);
        System.out.print("PARAMS:");
        for (Entry<String, String> entry : args.entrySet()) {
            System.out.print(" - " + entry.getKey() + "=" + entry.getValue());
        }
        //////////////////////////////// END DEBUG ////////////////////////////////
        
        // Create a new request object
        Request request = new Request(ip, method.name(), uri, args);
        
        // Check if the request is allowed
        if (!RequestManager.isAllowed(request)) {
            System.out.print("Denied query from: " + ip);
            return new NanoHTTPD.Response(Status.FORBIDDEN, MIME_PLAINTEXT, "403 FORBIDDEN");
        }
        
        // Get the resource
        Resource resource;
        if (uri.length() > 0) {
            resource = ResourceManager.getResource(uri.substring(1));
        }
        else {
            resource = ResourceManager.getDefault();
        }
        
        // Return 404 NOT FOUND - if resource cannot be found
        if (resource == null) {
            System.out.print("Invalid resource request from: " + ip + " : " + uri);
            return new NanoHTTPD.Response(Status.NOT_FOUND, MIME_PLAINTEXT, "404 NOT FOUND");
        }
        
        // Get a the result of the resource
        String result = resource.getResult(request);
        
        // Return the result
        return new NanoHTTPD.Response(result);
    }
}
