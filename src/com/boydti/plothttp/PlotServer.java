package com.boydti.plothttp;

import java.util.Map;

public class PlotServer extends NanoHTTPD {
    public PlotServer() {
        super(Main.port);
    }
    
    @Override public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        Map<String, String> parms = session.getParms();
        
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
