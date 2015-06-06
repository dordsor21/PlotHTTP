package com.boydti.plothttp.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import com.boydti.plothttp.util.JSONFormatter;
import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.intellectualcrafters.json.JSONArray;
import com.intellectualcrafters.json.JSONObject;
import com.intellectualcrafters.plot.PlotSquared;
import com.intellectualcrafters.plot.object.BlockLoc;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.util.ClusterManager;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.bukkit.UUIDHandler;

public class PlotResource extends Resource {

    // API for getting info of plots
    
    @Override
    public String toString() {
        return "plots";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(Request request, IHTTPSession session) {
        String world = request.ARGS.get("world");
        
        PlotId id = null;
        String idStr = request.ARGS.get("id");
        if (idStr != null) {
            id = PlotId.fromString(idStr);
        }
        
        Collection<Plot> plots;
        if (world != null) {
            if (id != null) {
                plots = new HashSet<Plot>();
                Plot plot = MainUtil.getPlot(world, id);
                if (plot != null) {
                    plots.add(plot);
                }
            }
            else {
                plots = PlotSquared.getPlots(world).values();
            }
        }
        else {
            if (id != null) {
                plots = new HashSet<Plot>();
                for (HashMap<PlotId, Plot> entry : PlotSquared.getAllPlotsRaw().values()) {
                    Plot plot = entry.get(id);
                    if (plot != null) {
                        plots.add(plot);
                    }
                }
            }
            else {
                plots = PlotSquared.getPlots();
            }
        }
        
        if (plots.size() == 0) { return null; }
        
        UUID uuid = getUUID(request.ARGS.get("owner"));
        if (uuid != null) {
            Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                Plot plot = i.next();
                if (plot.owner == null || !plot.owner.equals(uuid)) {
                    i.remove();
                }
            }
        }
        
        if (plots.size() == 0) { return null; }
        
        uuid = getUUID(request.ARGS.get("members"));
        if (uuid != null) {
            Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                Plot plot = i.next();
                if (!plot.members.contains(uuid)) {
                    i.remove();
                }
            }
        }
        
        if (plots.size() == 0) { return null; }
        
        uuid = getUUID(request.ARGS.get("allowed"));
        if (uuid != null) {
            Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                Plot plot = i.next();
                if (!plot.isAdded(uuid)) {
                    i.remove();
                }
            }
        }
        
        if (plots.size() == 0) { return null; }
        
        uuid = getUUID(request.ARGS.get("trusted"));
        if (uuid != null) {
            Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                Plot plot = i.next();
                if (!plot.trusted.contains(uuid)) {
                    i.remove();
                }
            }
        }
        
        if (plots.size() == 0) { return null; }
        
        uuid = getUUID(request.ARGS.get("denied"));
        if (uuid != null) {
            Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                Plot plot = i.next();
                if (!plot.denied.contains(uuid)) {
                    i.remove();
                }
            }
        }
        
        if (plots.size() == 0) { return null; }
        
        String clusterName = request.ARGS.get("cluster");
        if (clusterName != null) {
            Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                Plot plot = i.next();
                if (!ClusterManager.getCluster(plot).getName().equals(clusterName)) {
                    i.remove();
                }
            } 
        }
        
        if (plots.size() == 0) { return null; }
        
        String alias = request.ARGS.get("alias");
        if (alias != null) {
            Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                Plot plot = i.next();
                if (!alias.equals(plot.settings.getAlias())) {
                    i.remove();
                }
            }
        }
        
        if (plots.size() == 0) { return null; }
        
        String mergedStr = request.ARGS.get("merged");
        if (alias != null) {
            Iterator<Plot> i = plots.iterator();
            if (mergedStr.equals("true")) {
                while (i.hasNext()) {
                    Plot plot = i.next();
                    if (!plot.settings.isMerged()) {
                        i.remove();
                    }
                }
            }
            else {
                while (i.hasNext()) {
                    Plot plot = i.next();
                    if (plot.settings.isMerged()) {
                        i.remove();
                    }
                }
            }
        }
        
        if (plots.size() == 0) { return null; }
        
        JSONArray plotsObj = new JSONArray();
        for (Plot plot : plots) {
            plotsObj.put(serializePlot(plot));
        }
        
        return plotsObj.toString(1).getBytes();
    }
    
    public JSONObject serializePlot(Plot plot) {
        JSONObject obj = new JSONObject();
        
        JSONObject id = new JSONObject();
        id.put("x", plot.id.x);
        id.put("y", plot.id.y);
        obj.put("id", id);
        
        obj.put("world", plot.world);
        obj.put("ownerUUID", plot.owner);
        obj.put("ownerName", plot.owner == null ? "" : UUIDHandler.getName(plot.owner));
        
        obj.put("flags", getArray(plot.settings.flags));
        
        obj.put("trusted", getArray(plot.trusted));
        
        obj.put("members", getArray(plot.members));
        
        obj.put("denied", getArray(plot.denied));
        
        obj.put("merged", plot.settings.isMerged());
        
        obj.put("alias", plot.settings.getAlias());
        
        BlockLoc home = plot.settings.getPosition();
        JSONObject homeObj = new JSONObject();
        if (home == null) {
            homeObj.put("x", "");
            homeObj.put("y", "");
            homeObj.put("z", "");
        }
        else {
            homeObj.put("x", home.x);
            homeObj.put("y", home.y);
            homeObj.put("z", home.z);
        }
        obj.put("home", homeObj);
        return obj;
    }
}
