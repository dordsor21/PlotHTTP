package com.boydti.plothttp;


import com.plotsquared.core.configuration.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WebSettings extends Config {
    @Final
    public static String VERSION = null;

    @Comment("The port the server is running on")
    public static int PORT = 8080;
    @Comment("The public web url")
    public static String WEB_IP = "http://empcraft.com";
    @Comment("Log file")
    public static String LOG_FILE = "plothttp.log";

    @Create
    public static Whitelist WHITELIST;
    @Create
    public static Content CONTENT;
    @Create
    public static Api API;

    public static class Whitelist {
        @Comment("Whitelist specific IP addresses")
        public boolean ENABLED = true;
        public List<String> ALLOWED = new ArrayList<>(Collections.singletonList("127.0.0.1"));
    }

    public static class Content {
        @Comment("Serve content to whitelisted IPs")
        public boolean SERVE = true;
        @Comment("The max file upload size in bytes")
        public int MAX_UPLOAD = 200000000;
        @Comment("The file download format")
        public String FILENAME = "%world%-%id%-%player%";
        @Comment("The navigation markup for the web interface")
        public List<String> LINKS = new ArrayList<>(Arrays.asList("<a class=navlink href='https://www.spigotmc.org/resources/1177/'>Home</a>", "<a class=navlink href='https://github.com/IntellectualCrafters/PlotSquared/wiki'>Wiki</a>", "<a class=navlink href='https://github.com/IntellectualCrafters/PlotSquared/issues'>Report Issue</a>", "<a class=navlink href='https://discord.gg/ngZCzbU'>Support/Chat</a>"));
    }

    public static class Api {
        @Comment("Serve the web API to whitelisted IPs")
        public boolean SERVE = true;
    }

    public static void save(File file) {
        save(file, WebSettings.class);
    }

    public static void load(File file) {
        load(file, WebSettings.class);
    }
}
