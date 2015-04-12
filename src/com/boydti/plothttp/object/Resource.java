package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD.Response;

public abstract class Resource {
    
    @Override
    public abstract String toString();
    
    public abstract byte[] getResult(Request request);
    
    public void process(Response page) {}
}
