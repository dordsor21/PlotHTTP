package com.boydti.plothttp.util;

import java.util.HashMap;
import java.util.Set;

public class WebUtil {
    private static HashMap<String, String> pages = new HashMap<>();
    
    public static String getPage(String page) {
        return pages.get(page);
    }
    
    public static void addPage(String page, String content) {
        pages.put(page, content);
    }
    
    public static void removePage(String page) {
        pages.remove(page);
    }
    
    public static Set<String> getPages() {
        return pages.keySet();
    }
}
