package com.boydti.plothttp.util;

import com.boydti.plothttp.object.Request;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.bind.DatatypeConverter;

public class Logger extends DataOutputStream {
    StringBuffer buffer = new StringBuffer();
    ExecutorService executor = Executors.newSingleThreadExecutor();
    AtomicBoolean updated = new AtomicBoolean();
    AtomicBoolean waiting = new AtomicBoolean();
    boolean lineColor = false;

    public Logger(File file) throws FileNotFoundException {
        this(new FileOutputStream(file, true));
    }

    public Logger(OutputStream out) {
        super(out);
    }

    public void writeLine(String msg) {
        try {
            write(msg.getBytes());
            write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logFileRequest(UUID user, File file, Request request, NanoHTTPD.IHTTPSession session) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file.toPath());
                 DigestInputStream dis = new DigestInputStream(is, md)) {
            }
            byte[] digest = md.digest();
            String hash = DatatypeConverter.printHexBinary(digest);

            String msg = "User " + user + " requested file " + file.getName() + " with hash: " + hash;
            logRequest(msg, request, session);
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }
    }

    public void logRequest(String msg, Request request, NanoHTTPD.IHTTPSession session) {
        String rStr = "IP: " + request.IP + " URI: " + request.URI + " METHOD: " + request.METHOD + " ARGS: " + request.ARGS;
        String sStr = "HEADERS: " + session.getHeaders() + " COOKIES: " + session.getCookies().getCookies();
        writeLine(msg + "\nRequest:\n - " + rStr + "\nSession:\n - " + sStr);
    }


    @Override
    public void write(int b) throws IOException {
        buffer.append((char) b);
        if (b == '\n') {
            updated.set(true);
            if (waiting.compareAndSet(false, true)) {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            updated.set(false);
                            int len = buffer.length();
                            String content = buffer.substring(0, len);
                            buffer.delete(0, len);
                            Logger.super.write(content.getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            waiting.set(false);
                            if (updated.get() && waiting.compareAndSet(false, true)) {
                                executor.submit(this);
                            }
                        }
                    }
                });
            }
        } else {
            updated.lazySet(true);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        executor.shutdownNow();
        close();
    }
}
