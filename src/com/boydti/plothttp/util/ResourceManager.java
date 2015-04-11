package com.boydti.plothttp.util;

import java.util.HashMap;
import java.util.Set;

import com.boydti.plothttp.object.Resource;

public class ResourceManager {
    private static HashMap<String, Resource> resources = new HashMap<>();
    private static Resource defaultResource = null;
    
    public static Set<String> getResources() {
        return resources.keySet();
    }
    
    public static void addResource(Resource resource) {
        resources.put(resource.toString(), resource);
    }
    
    public static Resource getResource(String name) {
        return resources.get(name);
    }
    
    public static void setDefault(Resource resource) {
        defaultResource = resource;
    }
    
    public static Resource getDefault() {
        return defaultResource;
    }
}
