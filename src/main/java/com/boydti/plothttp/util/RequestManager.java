package com.boydti.plothttp.util;

import com.boydti.plothttp.object.Request;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class RequestManager {
    private ConcurrentHashMap<String, Request> whitelist = new ConcurrentHashMap<>();

    public boolean addToken(final Request request) {
        return whitelist.put(request.IP, request) != request;
    }

    public HashSet<Request> getTokens() {
        return new HashSet<Request>(whitelist.values());
    }

    public void clear() {
        whitelist.clear();
    }

    public boolean removeToken(final Request request) {
        return whitelist.remove(request.IP, request);
    }

    public boolean removeToken(final String ip) {
        return whitelist.remove(ip) != null;
    }


    public boolean isAllowed(final Request request) {
        Request white = whitelist.get(request.IP);
        if (white == null) {
            white = whitelist.get("*");
            if (white == null) {
                return false;
            }
        }
        if (white.allows(request)) {
            if (white.uses > 0) {
                white.uses--;
                if (white.uses == 0) {
                    whitelist.remove(white.IP);
                }
            }
            return true;
        }
        return false;
    }
}
