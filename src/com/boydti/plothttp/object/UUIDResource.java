package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;

public class UUIDResource extends Resource{

    // API for fetching information about UUIDS
    
    @Override
    public String toString() {
        return "uuids";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(Request request, IHTTPSession session) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
