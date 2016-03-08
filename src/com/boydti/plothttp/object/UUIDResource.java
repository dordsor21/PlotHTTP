package com.boydti.plothttp.object;

import java.util.UUID;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.intellectualcrafters.json.JSONArray;
import com.intellectualcrafters.json.JSONObject;
import com.intellectualcrafters.plot.util.UUIDHandler;

public class UUIDResource extends Resource {

    // API for fetching information about UUIDS

    @Override
    public String toString() {
        return "uuids";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(final Request request, final IHTTPSession session) {
        final JSONArray array = new JSONArray();
        for (final String arg : request.ARGS.keySet()) {
            final UUID uuid = getUUID(arg);
            final JSONObject obj = new JSONObject();
            if (uuid != null) {
                final String name = UUIDHandler.getName(uuid);
                if (name != null) {
                    obj.put("uuid", uuid.toString());
                    obj.put("name", name.toString());
                } else {
                    obj.put("uuid", uuid.toString());
                    obj.put("name", "");
                }
            } else {
                obj.put("uuid", "");
                obj.put("name", arg);
            }
            obj.put("input", arg);
            array.put(obj);
        }
        return array.toString(1).getBytes();
    }
}
