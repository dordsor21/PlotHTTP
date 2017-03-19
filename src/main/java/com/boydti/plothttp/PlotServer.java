package com.boydti.plothttp;

import com.boydti.plothttp.object.Request;
import com.boydti.plothttp.object.Resource;
import com.boydti.plothttp.util.NanoHTTPD;
import com.boydti.plothttp.util.NanoHTTPD.Response.Status;
import com.boydti.plothttp.util.RequestManager;
import com.boydti.plothttp.util.ResourceManager;
import java.io.ByteArrayInputStream;
import java.util.Map;

public class PlotServer extends NanoHTTPD {

    private RequestManager manager;

    public PlotServer() {
        super(Main.config().PORT);
        this.manager = new RequestManager();
    }

    public void setRequestManager(RequestManager manager) {
        this.manager = manager;
    }

    public RequestManager getRequestManager() {
        return manager;
    }

    @Override
    public Response serve(final IHTTPSession session) {
        try {
            final Map<String, String> headers = session.getHeaders();
            final String ip = headers.get("remote-addr");

            final Method method = session.getMethod();
            final String uri = session.getUri();
            final Map<String, String> args = session.getParms();

            //////////////////////////////// DEBUG STUFF ////////////////////////////////
            // System.out.print("IP: " + ip);
            // System.out.print("METHOD: " + method.name());
            // System.out.print("URI: " + uri);
            // System.out.print("PARAMS:");
            // for (Map.Entry<String, String> entry : args.entrySet()) {
            //     System.out.print(" - " + entry.getKey() + "=" + entry.getValue());
            // }
            //////////////////////////////// END DEBUG ////////////////////////////////

            // Create a new request object
            final Request request = new Request(ip, method.name(), uri, args);

            // Check if the request is allowed
            if (!manager.isAllowed(request)) {
                return new NanoHTTPD.Response(Status.FORBIDDEN, MIME_PLAINTEXT, "403 FORBIDDEN (1) ");
            }

            // Get the resource
            Resource resource;
            if (uri.length() > 0) {
                resource = ResourceManager.getResource(uri.substring(1));
            } else {
                resource = ResourceManager.getDefault();
            }

            // Return '404 NOT FOUND' - if resource cannot be found
            if (resource == null) {
                return new NanoHTTPD.Response(Status.NOT_FOUND, MIME_PLAINTEXT, "404 NOT FOUND (1)");
            }

            // Get a the result of the resource
            final byte[] result = resource.getResult(request, session);

            // Return '404 NOT FOUND' - if resource returns null
            if (result == null) {
                return new NanoHTTPD.Response(Status.NOT_FOUND, MIME_PLAINTEXT, "404 NOT FOUND (2)");
            }

            // Return an empty result
            if (result.length == 0) {
                return new NanoHTTPD.Response("[]");
            }

            // Return the result
            final ByteArrayInputStream data = new ByteArrayInputStream(result);
            final Response page = new NanoHTTPD.Response(Status.OK, MIME_HTML, data);
            resource.process(page);
            return page;
        } catch (Throwable e) {
            e.printStackTrace();
            return new NanoHTTPD.Response(Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }
}
