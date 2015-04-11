package com.boydti.plothttp.util;

import java.util.HashSet;

import com.boydti.plothttp.object.Request;

public class RequestManager {
    private static HashSet<Request> whitelist = new HashSet<>();
    
    public static boolean addToken(Request request) {
        return whitelist.add(request);
    }
    
    public static HashSet<Request> getTokens() {
        return new HashSet<Request>(whitelist);
    }
    
    public static boolean removeToken(Request request) {
        return whitelist.remove(request);
    }
    
    public static boolean isAllowed(Request request) {
        for (Request white : whitelist) {
            if (white.allows(request)) {
                return true;
            }
        }
        return false;
    }
}
