package com.boydti.plothttp.object;

import com.boydti.plothttp.util.WebUtil;

public class WebResource extends Resource{

    // TODO this will be an interactive webpage
    
    @Override
    public String toString() {
        return "web";
    }

    // will return an HTML web page
    @Override
    public String getResult(Request request) {
        String page = request.ARGS.get("page");
        if (page == null) {
            page = "index";
        }
        String result = WebUtil.getPage(page);
        return result;
    }
    
}
