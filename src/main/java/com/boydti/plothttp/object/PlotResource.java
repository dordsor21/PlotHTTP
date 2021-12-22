package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.location.BlockLoc;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotCluster;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.util.query.PlotQuery;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

public class PlotResource extends Resource {

    // API for getting info of plots

    @Override
    public String toString() {
        return "plots";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(final Request request, final IHTTPSession session) {
        final String world = request.ARGS.get("world");
        final String area = request.ARGS.get("area");
        final String baseString = request.ARGS.get("base");
        final boolean base = baseString == null ? false : Boolean.parseBoolean(baseString);
        PlotId id = null;
        final String idStr = request.ARGS.get("id");
        if (idStr != null) {
            id = PlotId.fromString(idStr);
        }
        Collection<Plot> plots = PlotQuery.newQuery().allPlots().asCollection();


        if (id != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                if (!plot.getId().equals(id)) {
                    i.remove();
                }
            }
        }
        if (area != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                if (!plot.getArea().toString().equals(area)) {
                    i.remove();
                }
            }
        }
        if (world != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                if (!plot.getArea().getWorldName().equals(world)) {
                    i.remove();
                }
            }
        }
        if (plots.size() == 0) {
            return null;
        }
        UUID uuid = getUUID(request.ARGS.get("owner"));
        if (uuid != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                if ((plot.getOwner() == null) || !plot.getOwner().equals(uuid)) {
                    i.remove();
                }
            }
        }
        if (plots.size() == 0) {
            return null;
        }
        uuid = getUUID(request.ARGS.get("members"));
        if (uuid != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                if (!plot.getMembers().contains(uuid)) {
                    i.remove();
                }
            }
        }
        if (plots.size() == 0) {
            return null;
        }
        uuid = getUUID(request.ARGS.get("allowed"));
        if (uuid != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                if (!plot.isAdded(uuid)) {
                    i.remove();
                }
            }
        }
        if (plots.size() == 0) {
            return null;
        }
        uuid = getUUID(request.ARGS.get("trusted"));
        if (uuid != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                if (!plot.getTrusted().contains(uuid)) {
                    i.remove();
                }
            }
        }
        if (plots.size() == 0) {
            return null;
        }
        uuid = getUUID(request.ARGS.get("denied"));
        if (uuid != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                if (!plot.getDenied().contains(uuid)) {
                    i.remove();
                }
            }
        }
        if (plots.size() == 0) {
            return null;
        }
        final String clusterName = request.ARGS.get("cluster");
        if (clusterName != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                PlotCluster cluster = plot.getCluster();
                if (cluster == null || !cluster.getName().equals(clusterName)) {
                    i.remove();
                }
            }
        }
        if (plots.size() == 0) {
            return null;
        }
        final String alias = request.ARGS.get("alias");
        if (alias != null) {
            final Iterator<Plot> i = plots.iterator();
            while (i.hasNext()) {
                final Plot plot = i.next();
                if (!alias.equals(plot.getSettings().getAlias())) {
                    i.remove();
                }
            }
        }
        if (plots.size() == 0) {
            return null;
        }
        final String mergedStr = request.ARGS.get("merged");
        if (alias != null) {
            final Iterator<Plot> i = plots.iterator();
            if (mergedStr.equals("true")) {
                while (i.hasNext()) {
                    final Plot plot = i.next();
                    if (!plot.isMerged()) {
                        i.remove();
                    }
                }
            } else {
                while (i.hasNext()) {
                    final Plot plot = i.next();
                    if (plot.isMerged()) {
                        i.remove();
                    }
                }
            }
        }
        if (plots.size() == 0) {
            return null;
        }
        final JSONArray plotsObj = new JSONArray();
        for (final Plot plot : plots) {
            plotsObj.add(serializePlot(plot));
        }
        return plotsObj.toString().getBytes();
    }

    public JSONObject serializePlot(final Plot plot) {
        final JSONObject obj = new JSONObject();

        final JSONObject id = new JSONObject();
        id.put("x", plot.getId().getX());
        id.put("y", plot.getId().getY());
        obj.put("id", id);

        obj.put("area", plot.getArea().toString());
        obj.put("world", plot.getArea().getWorldName());
        obj.put("ownerUUID", plot.getOwner().toString());
        obj.put("ownerName", plot.getOwner() == null ? "" :
                PlotSquared.get().getImpromptuUUIDPipeline().getImmediately(plot.getOwner()).getUsername());

        obj.put("flags", getArray(plot.getFlags()));

        obj.put("trusted", getArray(plot.getTrusted()));

        obj.put("members", getArray(plot.getMembers()));

        obj.put("denied", getArray(plot.getDenied()));

        obj.put("merged", plot.isMerged());

        obj.put("alias", plot.getSettings().getAlias());

        final BlockLoc home = plot.getSettings().getPosition();
        final JSONObject homeObj = new JSONObject();
        if (home == null) {
            homeObj.put("x", "");
            homeObj.put("y", "");
            homeObj.put("z", "");
        } else {
            homeObj.put("x", home.getX());
            homeObj.put("y", home.getY());
            homeObj.put("z", home.getZ());
        }
        obj.put("home", homeObj);
        return obj;
    }

}
