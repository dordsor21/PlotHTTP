package com.boydti.plothttp.object;

import java.util.Map;
import java.util.Map.Entry;

public class Request {
    public final String IP;
    public final String METHOD;
    public final String URI;
    public final Map<String, String> ARGS;
    
    public Request(String ip, String method, String uri, Map<String, String> args) {
        this.IP = ip;
        this.METHOD = method;
        this.URI = uri;
        this.ARGS = args;
    }
    
    public int hashCode() {
        int hash = 31;
        hash = 31 * hash + IP.hashCode();
        hash = 31 * hash + METHOD.hashCode();
        hash = 31 * hash + URI.hashCode();
        hash = 31 * hash + ARGS.size();
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Request other = (Request) obj;
        return (other.IP.equals(IP) && other.METHOD.equals(METHOD) && other.URI.equals(URI) && other.ARGS.equals(ARGS));
    }
    
    public boolean allows(Request other) {
        if (!other.IP.equals(IP) && !IP.equals("*")) {
            return false;
        }
        if (!other.IP.equals(METHOD) && !METHOD.equals("*")) {
            return false;
        }
        if (!other.IP.equals(URI) && !URI.equals("*")) {
            return false;
        }
        if (!other.ARGS.equals(ARGS)) {
            if (ARGS.containsKey("*")) {
                return true;
            }
            for (Entry<String, String> entry : other.ARGS.entrySet()) {
                String current = ARGS.get(entry.getKey());
                if (current == null) {
                    return false;
                }
                if (!current.equals(entry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }
    
}
