package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.UUID;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.boydti.plothttp.util.NanoHTTPD.Response;
import com.github.intellectualsites.plotsquared.json.JSONArray;
import com.github.intellectualsites.plotsquared.plot.util.UUIDHandler;

public abstract class Resource {

    @Override
    public abstract String toString();

    protected abstract byte[] getResult(Request request, IHTTPSession session);

    public Response getResponse(Request request, IHTTPSession session) {
        // Get a the result of the resource
        final byte[] result = getResult(request, session);

        // Return '404 NOT FOUND' - if resource returns null
        if (result == null) {
            return new NanoHTTPD.Response(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "404 NOT FOUND (2)");
        }

        // Return an empty result
        if (result.length == 0) {
            return new NanoHTTPD.Response("[]");
        }

        // Return the result
        final ByteArrayInputStream data = new ByteArrayInputStream(result);
        final Response page = new NanoHTTPD.Response(Response.Status.OK, NanoHTTPD.MIME_HTML, data);
        process(page);
        return page;
    }

    public void onSuccess(final Response page) {

    }

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
