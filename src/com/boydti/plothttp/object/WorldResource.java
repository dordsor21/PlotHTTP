package com.boydti.plothttp.object;

import java.util.Collection;
import java.util.HashSet;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.intellectualcrafters.json.JSONArray;
import com.intellectualcrafters.json.JSONObject;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.PlotWorld;
import com.intellectualcrafters.plot.util.SetupUtils;

public class WorldResource extends Resource {

    // API class for fetching information about worlds

    @Override
    public String toString() {
        return "worlds";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(final Request request, final IHTTPSession session) {
        final String world = request.ARGS.get("world");
        Collection<PlotWorld> worlds;
        if (world != null) {
            worlds = new HashSet<PlotWorld>();
            final PlotWorld plotworld = PS.get().getPlotWorld(world);
            if (plotworld != null) {
                worlds.add(plotworld);
            }
        } else {
            worlds = PS.get().getPlotWorldObjects();
        }

        if (worlds.size() == 0) {
            return null;
        }

        final JSONArray worldsObj = new JSONArray();
        for (final PlotWorld plotworld : worlds) {
            worldsObj.put(serializePlotWorld(plotworld));
        }

        return worldsObj.toString(1).getBytes();
    }

    public JSONObject serializePlotWorld(final PlotWorld pw) {
        final JSONObject obj = new JSONObject();
        obj.put("worldname", pw.worldname);
        final String generator = SetupUtils.manager.getGenerator(pw);
        if (generator == null) {
            obj.put("generator", "");
        } else {
            obj.put("generator", generator);
        }
        obj.put("terrain", pw.TERRAIN);
        obj.put("type", pw.TYPE);
        final JSONObject priceObj = new JSONObject();
        obj.put("economy", pw.USE_ECONOMY);
        priceObj.put("buy", pw.PLOT_PRICE);
        priceObj.put("sell", pw.SELL_PRICE);
        obj.put("price", priceObj);
        final JSONObject homeObj = new JSONObject();
        if (pw.DEFAULT_HOME == null) {
            homeObj.put("default", "side");
        } else {
            if (pw.DEFAULT_HOME.x == Integer.MAX_VALUE) {
                homeObj.put("default", "center");
            } else {
                homeObj.put("default", pw.DEFAULT_HOME.x + "," + pw.DEFAULT_HOME.z);
            }
        }
        homeObj.put("allow-nonmember", pw.HOME_ALLOW_NONMEMBER);
        obj.put("home", homeObj);
        obj.put("pve", pw.PVE);
        obj.put("pvp", pw.PVP);
        obj.put("mob-spawning", pw.MOB_SPAWNING);
        obj.put("allow-signs", pw.ALLOW_SIGNS);
        obj.put("auto-merge", pw.AUTO_MERGE);
        obj.put("flags", getArray(pw.DEFAULT_FLAGS.values()));
        final JSONObject spawnObj = new JSONObject();
        spawnObj.put("breeding", pw.SPAWN_BREEDING);
        spawnObj.put("custom", pw.SPAWN_CUSTOM);
        spawnObj.put("custom", pw.SPAWN_EGGS);
        obj.put("spawn", spawnObj);
        obj.put("world-border", pw.WORLD_BORDER);
        return obj;
    }

}
