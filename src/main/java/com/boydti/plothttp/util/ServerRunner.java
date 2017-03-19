package com.boydti.plothttp.util;

import java.io.IOException;

public class ServerRunner {
    public static <T extends NanoHTTPD> T run(final Class<T> serverClass) {
        try {
            final T instance = serverClass.newInstance();
            executeInstance(instance);
            return instance;
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void executeInstance(final NanoHTTPD server) {
        try {
            server.start();
        } catch (final IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }
    }
}
