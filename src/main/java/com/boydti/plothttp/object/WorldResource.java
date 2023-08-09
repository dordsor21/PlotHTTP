package com.boydti.plothttp.object;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.util.StringMan;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
            areas = new HashSet<>(Arrays.asList(PlotSquared.get().getPlotAreaManager().getAllPlotAreas()));
            if (id != null) {
                Iterator<PlotArea> iter = areas.iterator();
                while (iter.hasNext()) {
                    if (!StringMan.isEqual(iter.next().getId(), id)) {
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
                    if (!StringMan.isEqual(iter.next().getWorldName(), world)) {
                        iter.remove();
                    }
                }
            }
        } else {
            PlotArea pa = PlotSquared.get().getPlotAreaManager().getPlotAreaByString(area);
            areas = pa == null ? new HashSet<>() : Collections.singletonList(pa);
        }
        if (areas.size() == 0) {
            return null;
        }
        final JSONArray worldsObj = new JSONArray();
        for (final PlotArea plotworld : areas) {
            worldsObj.add(serializePlotArea(plotworld));
        }
        return worldsObj.toString().getBytes();
    }

    public JSONObject serializePlotArea(final PlotArea pw) {
        final JSONObject obj = new JSONObject();
        obj.put("worldname", pw.getWorldName());
        final String generator = pw.getGenerator().getName();
        if (generator == null) {
            obj.put("generator", "");
        } else {
            obj.put("generator", generator);
        }
        obj.put("terrain", pw.getTerrain());
        obj.put("type", pw.getType());
        obj.put("economy", pw.useEconomy());
        obj.put("price", pw.getPrices());
        final JSONObject homeObj = new JSONObject();
        if (pw.defaultHome() == null) {
            homeObj.put("default", "side");
        } else {
            if (pw.defaultHome().getX() == Integer.MAX_VALUE) {
                homeObj.put("default", "center");
            } else {
                homeObj.put("default", pw.defaultHome().getX() + "," + pw.defaultHome().getZ());
            }
        }
        homeObj.put("allow-nonmember", pw.isHomeAllowNonmember());
        obj.put("home", homeObj);
        obj.put("mob-spawning", pw.isMobSpawning());
        obj.put("allow-signs", pw.allowSigns());
        obj.put("auto-merge", pw.isAutoMerge());
        obj.put("flags", getArray(pw.getFlagContainer().getFlagMap().values()));
        final JSONObject spawnObj = new JSONObject();
        spawnObj.put("breeding", pw.isSpawnBreeding());
        spawnObj.put("custom", pw.isSpawnCustom());
        spawnObj.put("eggs", pw.isSpawnEggs());
        obj.put("spawn", spawnObj);
        obj.put("world-border", pw.hasWorldBorder());
        return obj;
    }

}
