package com.boydti.plothttp.util;

import java.util.HashSet;
import java.util.Iterator;

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
        Iterator<Request> i = whitelist.iterator();
        while (i.hasNext()) {
            Request white = i.next();
            if (white.allows(request)) {
                if (white.uses > 0) {
                    white.uses--;
                    if (white.uses == 0) {
                        i.remove();
                    }
                }
                return true;
            }
        }
        return false;
    }
}
