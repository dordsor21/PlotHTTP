package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;

public class WorldResource extends Resource{

    // API class for fetching information about worlds
    
    @Override
    public String toString() {
        return "worlds";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(Request request, IHTTPSession session) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
