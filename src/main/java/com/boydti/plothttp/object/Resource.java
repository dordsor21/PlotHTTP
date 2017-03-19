package com.boydti.plothttp.object;

import java.util.Collection;
import java.util.UUID;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.boydti.plothttp.util.NanoHTTPD.Response;
import com.intellectualcrafters.json.JSONArray;
import com.intellectualcrafters.plot.util.UUIDHandler;

public abstract class Resource {

    @Override
    public abstract String toString();

    public abstract byte[] getResult(Request request, IHTTPSession session);

    public void process(final Response page) {
    }

    public JSONArray getArray(final Collection<?> collection) {
        final JSONArray array = new JSONArray();
        for (final Object object : collection) {
            array.put(object.toString());
        }
        return array;
    }

    public JSONArray getArray(final Object[] collection) {
        final JSONArray array = new JSONArray();
        for (final Object object : collection) {
            array.put(object.toString());
        }
        return array;
    }

    public UUID getUUID(final String name) {
        if (name == null) {
            return null;
        }
        final UUID uuid = UUIDHandler.getUUID(name, null);
        if (uuid != null) {
            return uuid;
        }
        try {
            return UUID.fromString(name);
        } catch (final Exception e) {
            return null;
        }
    }
}
