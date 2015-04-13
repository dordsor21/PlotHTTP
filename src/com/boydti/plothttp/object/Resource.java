package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;
import com.boydti.plothttp.util.NanoHTTPD.Response;

public abstract class Resource {
    
    @Override
    public abstract String toString();
    
    public abstract byte[] getResult(Request request, IHTTPSession session);
    
    public void process(Response page) {}
}
