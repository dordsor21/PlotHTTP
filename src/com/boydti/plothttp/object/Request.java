package com.boydti.plothttp.object;

import java.util.Map;
import java.util.Map.Entry;

public class Request {
    public final String IP;
    public final String METHOD;
    public final String URI;
    public final Map<String, String> ARGS;
    public int uses;

    public Request(final String ip, final String method, final String uri, final Map<String, String> args) {
        this.IP = ip;
        this.METHOD = method;
        this.URI = uri;
        this.ARGS = args;
        this.uses = -1;
    }

    public Request(final String ip, final String method, final String uri, final Map<String, String> args, final int uses) {
        this.IP = ip;
        this.METHOD = method;
        this.URI = uri;
        this.ARGS = args;
        this.uses = uses;
    }

    @Override
    public int hashCode() {
        int hash = 31;
        hash = (31 * hash) + this.IP.hashCode();
        hash = (31 * hash) + this.METHOD.hashCode();
        hash = (31 * hash) + this.URI.hashCode();
        hash = (31 * hash) + this.ARGS.size();
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
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
        return (other.IP.equals(this.IP) && other.METHOD.equals(this.METHOD) && other.URI.equals(this.URI) && other.ARGS.equals(this.ARGS));
    }

    public boolean allows(final Request other) {
        if (!other.IP.equals(this.IP) && !this.IP.equals("*")) {
            return false;
        }
        if (!other.METHOD.equals(this.METHOD) && !this.METHOD.equals("*")) {
            return false;
        }
        if (!other.URI.equals(this.URI) && !this.URI.equals("*")) {
            return false;
        }
        if (!other.ARGS.equals(this.ARGS)) {
            if (this.ARGS.containsKey("*")) {
                return true;
            }
            for (final Entry<String, String> entry : this.ARGS.entrySet()) {
                final String current = other.ARGS.get(entry.getKey());
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
