package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;

public class ClusterResource extends Resource{

    // API for getting info about clusters
    
    @Override
    public String toString() {
        return "clusters";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(Request request, IHTTPSession session) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
