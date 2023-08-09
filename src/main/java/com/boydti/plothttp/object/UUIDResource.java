package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.plotsquared.core.PlotSquared;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.UUID;

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
                final String name = PlotSquared.get().getImpromptuUUIDPipeline().getImmediately(uuid).username();
                if (name != null) {
                    obj.put("uuid", uuid.toString());
                    obj.put("name", name);
                } else {
                    obj.put("uuid", uuid.toString());
                    obj.put("name", "");
                }
            } else {
                obj.put("uuid", "");
                obj.put("name", arg);
            }
            obj.put("input", arg);
            array.add(obj);
        }
        return array.toString().getBytes();
    }

}
