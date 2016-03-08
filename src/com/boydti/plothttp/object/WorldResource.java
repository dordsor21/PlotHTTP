package com.boydti.plothttp.object;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.intellectualcrafters.json.JSONArray;
import com.intellectualcrafters.json.JSONObject;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.util.SetupUtils;
import com.intellectualcrafters.plot.util.StringMan;

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
        final String area = request.ARGS.get("area");
        final String id = request.ARGS.get("id");
        final Collection<PlotArea> areas;
        if (area == null) {
            areas = new HashSet<>(PS.get().getPlotAreas());
            if (id != null) {
                Iterator<PlotArea> iter = areas.iterator();
                while (iter.hasNext()) {
                    if (!StringMan.isEqual(iter.next().id, id)) {
                        iter.remove();
                    }
                }
            }
            if (areas.size() == 0) {
                return null;
            }
            if (world != null) {
                Iterator<PlotArea> iter = areas.iterator();
                while (iter.hasNext()) {
                    if (!StringMan.isEqual(iter.next().worldname, world)) {
                        iter.remove();
                    }
                }
            }
        } else {
            PlotArea pa = PS.get().getPlotAreaByString(area);
            areas = pa == null ? new HashSet<PlotArea>() : Collections.singletonList(pa);
        }
        if (areas.size() == 0) {
            return null;
        }
        final JSONArray worldsObj = new JSONArray();
        for (final PlotArea plotworld : areas) {
            worldsObj.put(serializePlotArea(plotworld));
        }
        return worldsObj.toString(1).getBytes();
    }

    public JSONObject serializePlotArea(final PlotArea pw) {
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
        obj.put("economy", pw.USE_ECONOMY);
        obj.put("price", pw.PRICES);
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
