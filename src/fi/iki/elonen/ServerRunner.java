package fi.iki.elonen;

import java.io.IOException;

import com.boydti.plothttp.NanoHTTPD;

public class ServerRunner {
    public static NanoHTTPD run(Class serverClass) {
        try {
            NanoHTTPD instance = (NanoHTTPD) serverClass.newInstance();
            executeInstance(instance);
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void executeInstance(NanoHTTPD server) {
        try {
            server.start();
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }
        System.out.println("Server started.");
    }
}
