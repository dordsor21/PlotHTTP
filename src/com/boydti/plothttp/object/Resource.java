package com.boydti.plothttp.object;

import java.util.Collection;
import java.util.UUID;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.boydti.plothttp.util.NanoHTTPD.Response;
import com.intellectualcrafters.json.JSONArray;
import com.intellectualcrafters.plot.util.bukkit.UUIDHandler;

public abstract class Resource {
    
    @Override
    public abstract String toString();
    
    public abstract byte[] getResult(Request request, IHTTPSession session);
    
    public void process(Response page) {}
    
    public JSONArray getArray(Collection<?> collection) {
        JSONArray array = new JSONArray();
        for (Object object : collection) {
            array.put(object.toString());
        }
        return array;
    }
    
    public JSONArray getArray(Object[] collection) {
        JSONArray array = new JSONArray();
        for (Object object : collection) {
            array.put(object.toString());
        }
        return array;
    }
    
    public UUID getUUID(String name) {
        if (name == null) {
            return null;
        }
        UUID uuid = UUIDHandler.getUUID(name);
        if (uuid != null) {
            return uuid;
        }
        try {
            return UUID.fromString(name);
        }
        catch(Exception e) {
            return null;
        }
    }
}
