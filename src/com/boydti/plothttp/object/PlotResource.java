package com.boydti.plothttp.object;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.boydti.plothttp.util.JSONFormatter;
import com.intellectualcrafters.json.JSONArray;
import com.intellectualcrafters.json.JSONObject;
import com.intellectualcrafters.plot.PlotSquared;
import com.intellectualcrafters.plot.object.BlockLoc;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.util.bukkit.UUIDHandler;

public class PlotResource extends Resource {

    // API for getting info of plots
    
    @Override
    public String toString() {
        return "plots";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(Request request) {
        /*
         * id
         * player
         * uuid
         * allowed
         * helper
         * trusted
         * denied
         * cluster
         * alias 
         */
        
        String world = request.ARGS.get("world");
        Collection<Plot> plots;
        if (world != null) {
            plots = PlotSquared.getPlots(world).values();
        }
        else {
            plots = PlotSquared.getPlots();
        }
        
        // TODO filter
        
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
        
        obj.put("helpers", getArray(plot.helpers));
        
        obj.put("trusted", getArray(plot.trusted));
        
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
    
    public JSONArray getArray(Collection<?> collection) {
        JSONArray array = new JSONArray();
        for (Object object : collection) {
            array.put(object.toString());
        }
        return array;
    }
}
