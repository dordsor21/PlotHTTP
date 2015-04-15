package com.boydti.plothttp.object;

import java.util.UUID;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.intellectualcrafters.json.JSONArray;
import com.intellectualcrafters.json.JSONObject;
import com.intellectualcrafters.plot.util.bukkit.UUIDHandler;

public class UUIDResource extends Resource{

    // API for fetching information about UUIDS
    
    @Override
    public String toString() {
        return "uuids";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(Request request, IHTTPSession session) {
        JSONArray array = new JSONArray();
        for (String arg : request.ARGS.keySet()) {
            UUID uuid = getUUID(arg);
            JSONObject obj = new JSONObject();
            if (uuid != null) {
                String name = UUIDHandler.getName(uuid);
                if (name != null) {
                    obj.put("uuid", uuid.toString());
                    obj.put("name", name.toString());
                }
                else {
                    obj.put("uuid", uuid.toString());
                    obj.put("name", "");
                }
            }
            else {
                obj.put("uuid", "");
                obj.put("name", arg);
            }
            obj.put("input", arg);
            array.put(obj);
        }
        return array.toString(1).getBytes();
    }
}
