package com.boydti.plothttp.object;

import com.boydti.plothttp.util.NanoHTTPD.IHTTPSession;

public class CommentResource extends Resource{

    // API for getting info of comments
    
    @Override
    public String toString() {
        return "comments";
    }

    // will return JSON object as String
    @Override
    public byte[] getResult(Request request, IHTTPSession session) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
