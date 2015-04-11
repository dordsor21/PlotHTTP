package com.boydti.plothttp.object;

public abstract class Resource {
    
    @Override
    public abstract String toString();
    
    public abstract String getResult(Request request);
}
