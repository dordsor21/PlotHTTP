package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;

public class SchematicResource extends Resource {

    /*
     *  API for fetching a schematic
     *  - Allows for returning raw schematic data (of an already saved schematic)
     *  - Allows for returning a list of schematics
     */

    @Override
    public String toString() {
        return "schematics";
    }

    // may or may not return JSON object as String
    @Override
    public byte[] getResult(final Request request, final IHTTPSession session) {
        // TODO Auto-generated method stub
        return null;
    }

}
