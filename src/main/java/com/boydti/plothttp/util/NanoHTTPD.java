package com.boydti.plothttp.util;

import com.boydti.plothttp.Main;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * A simple, tiny, nicely embeddable HTTP server in Java
 * <p/>
 * <p/>
 * NanoHTTPD
 * <p></p>Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias</p>
 * <p/>
 * <p/>
 * <b>Features + limitations: </b>
 * <ul>
 * <p/>
 * <li>Only one Java file</li>
 * <li>Java 5 compatible</li>
 * <li>Released as open source, Modified BSD licence</li>
 * <li>No fixed config files, logging, authorization etc. (Implement yourself if you need them.)</li>
 * <li>Supports parameter parsing of GET and POST methods (+ rudimentary PUT support in 1.25)</li>
 * <li>Supports both dynamic content and file serving</li>
 * <li>Supports file upload (since version 1.2, 2010)</li>
 * <li>Supports partial content (streaming)</li>
 * <li>Supports ETags</li>
 * <li>Never caches anything</li>
 * <li>Doesn't limit bandwidth, request time or simultaneous connections</li>
 * <li>Default code serves files and shows all HTTP parameters and headers</li>
 * <li>File server supports directory listing, index.html and index.htm</li>
 * <li>File server supports partial content (streaming)</li>
 * <li>File server supports ETags</li>
 * <li>File server does the 301 redirection trick for directories without '/'</li>
 * <li>File server supports simple skipping for files (continue download)</li>
 * <li>File server serves also very long files without memory overhead</li>
 * <li>Contains a built-in list of most common mime types</li>
 * <li>All header names are converted lowercase so they don't vary between browsers/clients</li>
 * <p/>
 * </ul>
 * <p/>
 * <p/>
 * <b>How to use: </b>
 * <ul>
 * <p/>
 * <li>Subclass and implement serve() and embed to your own program</li>
 * <p/>
 * </ul>
 * <p/>
 * See the separate "LICENSE.md" file for the distribution license (Modified BSD licence)
 */
public abstract class NanoHTTPD {
    /**
     * Maximum time to wait on Socket.getInputStream().read() (in milliseconds)
     * This is required as the Keep-Alive HTTP connections would otherwise
     * block the socket reading thread forever (or as long the browser is open).
     */
    public static final int SOCKET_READ_TIMEOUT = 5000;
    /**
     * Common mime type for dynamic content: plain text
     */
    public static final String MIME_PLAINTEXT = "text/plain";
    /**
     * Common mime type for dynamic content: html
     */
    public static final String MIME_HTML = "text/html";
    /**
     * Pseudo-Parameter to use to store the actual query string in the parameters map for later re-processing.
     */
    private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";
    private final String hostname;
    private final int myPort;
    private ServerSocket myServerSocket;
    private final Set<Socket> openConnections = new HashSet<Socket>();
    private Thread myThread;
    /**
     * Pluggable strategy for asynchronously executing requests.
     */
    private AsyncRunner asyncRunner;
    /**
     * Pluggable strategy for creating and cleaning up temporary files.
     */
    private TempFileManagerFactory tempFileManagerFactory;

    /**
     * Constructs an HTTP server on given port.
     */
    public NanoHTTPD(final int port) {
        this(null, port);
    }

    /**
     * Constructs an HTTP server on given hostname and port.
     */
    public NanoHTTPD(final String hostname, final int port) {
        this.hostname = hostname;
        this.myPort = port;
        setTempFileManagerFactory(new PlotFileManager());
        setAsyncRunner(new DefaultAsyncRunner());
    }

    private static final void safeClose(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
            }
        }
    }

    private static final void safeClose(final Socket closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
            }
        }
    }

    private static final void safeClose(final ServerSocket closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException e) {
            }
        }
    }

    /**
     * Start the server.
     *
     * @throws IOException if the socket is in use.
     */
    public void start() throws IOException {
        this.myServerSocket = new ServerSocket();
        this.myServerSocket.bind((this.hostname != null) ? new InetSocketAddress(this.hostname, this.myPort) : new InetSocketAddress(this.myPort));

        this.myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        final Socket finalAccept = NanoHTTPD.this.myServerSocket.accept();
                        registerConnection(finalAccept);
                        finalAccept.setSoTimeout(SOCKET_READ_TIMEOUT);
                        final InputStream inputStream = finalAccept.getInputStream();
                        NanoHTTPD.this.asyncRunner.exec(new Runnable() {
                            @Override
                            public void run() {
                                OutputStream outputStream = null;
                                try {
                                    outputStream = finalAccept.getOutputStream();
                                    final TempFileManager tempFileManager = NanoHTTPD.this.tempFileManagerFactory.create();
                                    final HTTPSession session = new HTTPSession(tempFileManager, inputStream, outputStream, finalAccept.getInetAddress());
                                    while (!finalAccept.isClosed()) {
                                        session.execute();
                                    }
                                } catch (final Exception e) {
                                    // When the socket is closed by the client, we throw our own SocketException
                                    // to break the  "keep alive" loop above.
                                    if (!((e instanceof SocketException) && "NanoHttpd Shutdown".equals(e.getMessage()))) {
                                        e.printStackTrace();
                                    }
                                } finally {
                                    safeClose(outputStream);
                                    safeClose(inputStream);
                                    safeClose(finalAccept);
                                    unRegisterConnection(finalAccept);
                                }
                            }
                        });
                    } catch (final IOException e) {
                    }
                } while (!NanoHTTPD.this.myServerSocket.isClosed());
            }
        });
        this.myThread.setDaemon(true);
        this.myThread.setName("NanoHttpd Main Listener");
        this.myThread.start();
    }

    /**
     * Stop the server.
     */
    public void stop() {
        try {
            safeClose(this.myServerSocket);
            closeAllConnections();
            if (this.myThread != null) {
                this.myThread.join();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers that a new connection has been set up.
     *
     * @param socket the {@link Socket} for the connection.
     */
    public synchronized void registerConnection(final Socket socket) {
        this.openConnections.add(socket);
    }

    /**
     * Registers that a connection has been closed
     *
     * @param socket
     *            the {@link Socket} for the connection.
     */
    public synchronized void unRegisterConnection(final Socket socket) {
        this.openConnections.remove(socket);
    }

    /**
     * Forcibly closes all connections that are open.
     */
    public synchronized void closeAllConnections() {
        for (final Socket socket : this.openConnections) {
            safeClose(socket);
        }
    }

    public final int getListeningPort() {
        return this.myServerSocket == null ? -1 : this.myServerSocket.getLocalPort();
    }

    public final boolean wasStarted() {
        return (this.myServerSocket != null) && (this.myThread != null);
    }

    public final boolean isAlive() {
        return wasStarted() && !this.myServerSocket.isClosed() && this.myThread.isAlive();
    }

    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this delegates to serveFile() and allows directory listing.)
     *
     * @param uri     Percent-decoded URI without parameters, for example "/index.cgi"
     * @param method  "GET", "POST" etc.
     * @param parms   Parsed, percent decoded parameters from URI and, in case of POST, data.
     * @param headers Header entries, percent decoded
     * @return HTTP response, see class Response for details
     */
    @Deprecated
    public Response serve(final String uri, final Method method, final Map<String, String> headers, final Map<String, String> parms, final Map<String, String> files) {
        return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }

    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this delegates to serveFile() and allows directory listing.)
     *
     * @param session The HTTP session
     * @return HTTP response, see class Response for details
     */
    public Response serve(final IHTTPSession session) {
        final Map<String, String> files = new HashMap<String, String>();
        final Method method = session.getMethod();
        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
            try {
                session.parseBody(files);
            } catch (final IOException ioe) {
                return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (final ResponseException re) {
                return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }

        final Map<String, String> parms = session.getParms();
        parms.put(QUERY_STRING_PARAMETER, session.getQueryParameterString());
        return serve(session.getUri(), method, session.getHeaders(), parms, files);
    }

    /**
     * Called when response is a success
     * @param response
     */
    public void success(Response response) {

    }

    /**
     * Decode percent encoded <code>String</code> values.
     *
     * @param str the percent encoded <code>String</code>
     * @return expanded form of the input, for example "foo%20bar" becomes "foo bar"
     */
    protected String decodePercent(final String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (final UnsupportedEncodingException ignored) {
        }
        return decoded;
    }

    /**
     * Decode parameters from a URL, handing the case where a single parameter name might have been
     * supplied several times, by return lists of values.  In general these lists will contain a single
     * element.
     *
     * @param parms original <b>NanoHttpd</b> parameters values, as passed to the <code>serve()</code> method.
     * @return a map of <code>String</code> (parameter name) to <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected Map<String, List<String>> decodeParameters(final Map<String, String> parms) {
        return this.decodeParameters(parms.get(QUERY_STRING_PARAMETER));
    }

    /**
     * Decode parameters from a URL, handing the case where a single parameter name might have been
     * supplied several times, by return lists of values.  In general these lists will contain a single
     * element.
     *
     * @param queryString a query string pulled from the URL.
     * @return a map of <code>String</code> (parameter name) to <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected Map<String, List<String>> decodeParameters(final String queryString) {
        final Map<String, List<String>> parms = new HashMap<String, List<String>>();
        if (queryString != null) {
            final StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                final String e = st.nextToken();
                final int sep = e.indexOf('=');
                final String propertyName = (sep >= 0) ? decodePercent(e.substring(0, sep)).trim() : decodePercent(e).trim();
                if (!parms.containsKey(propertyName)) {
                    parms.put(propertyName, new ArrayList<String>());
                }
                final String propertyValue = (sep >= 0) ? decodePercent(e.substring(sep + 1)) : null;
                if (propertyValue != null) {
                    parms.get(propertyName).add(propertyValue);
                }
            }
        }
        return parms;
    }

    // ------------------------------------------------------------------------------- //
    //
    // Threading Strategy.
    //
    // ------------------------------------------------------------------------------- //

    /**
     * Pluggable strategy for asynchronously executing requests.
     *
     * @param asyncRunner new strategy for handling threads.
     */
    public void setAsyncRunner(final AsyncRunner asyncRunner) {
        this.asyncRunner = asyncRunner;
    }

    // ------------------------------------------------------------------------------- //
    //
    // Temp file handling strategy.
    //
    // ------------------------------------------------------------------------------- //

    /**
     * Pluggable strategy for creating and cleaning up temporary files.
     *
     * @param tempFileManagerFactory new strategy for handling temp files.
     */
    public void setTempFileManagerFactory(final TempFileManagerFactory tempFileManagerFactory) {
        this.tempFileManagerFactory = tempFileManagerFactory;
    }

    /**
     * HTTP Request methods, with the ability to decode a <code>String</code> back to its enum value.
     */
    public enum Method {
        GET,
        PUT,
        POST,
        DELETE,
        HEAD,
        OPTIONS;

        static Method lookup(final String method) {
            for (final Method m : Method.values()) {
                if (m.toString().equalsIgnoreCase(method)) {
                    return m;
                }
            }
            return null;
        }
    }

    /**
     * Pluggable strategy for asynchronously executing requests.
     */
    public interface AsyncRunner {
        void exec(Runnable code);
    }

    /**
     * Factory to create temp file managers.
     */
    public interface TempFileManagerFactory {
        TempFileManager create();
    }

    // ------------------------------------------------------------------------------- //

    /**
     * Temp file manager.
     * <p/>
     * <p>Temp file managers are created 1-to-1 with incoming requests, to create and cleanup
     * temporary files created as a result of handling the request.</p>
     */
    public interface TempFileManager {
        TempFile createTempFile() throws Exception;

        void clear();
    }

    /**
     * A temp file.
     * <p/>
     * <p>Temp files are responsible for managing the actual temporary storage and cleaning
     * themselves up when no longer needed.</p>
     */
    public interface TempFile {
        OutputStream open() throws Exception;

        void delete() throws Exception;

        String getName();
    }

    /**
     * Default threading strategy for NanoHttpd.
     * <p/>
     * <p>By default, the server spawns a new Thread for every incoming request.  These are set
     * to <i>daemon</i> status, and named according to the request number.  The name is
     * useful when profiling the application.</p>
     */
    public static class DefaultAsyncRunner implements AsyncRunner {
        private long requestCount;

        @Override
        public void exec(final Runnable code) {
            ++this.requestCount;
            final Thread t = new Thread(code);
            t.setDaemon(true);
            t.setName("NanoHttpd Request Processor (#" + this.requestCount + ")");
            t.start();
        }
    }

    /**
     * Default strategy for creating and cleaning up temporary files.
     * <p/>
     * <p></p>This class stores its files in the standard location (that is,
     * wherever <code>java.io.tmpdir</code> points to).  Files are added
     * to an internal list, and deleted when no longer needed (that is,
     * when <code>clear()</code> is invoked at the end of processing a
     * request).</p>
     */
    public static class DefaultTempFileManager implements TempFileManager {
        private final String tmpdir;
        private final List<TempFile> tempFiles;

        public DefaultTempFileManager() {
            this.tmpdir = System.getProperty("java.io.tmpdir");
            this.tempFiles = new ArrayList<TempFile>();
        }

        @Override
        public TempFile createTempFile() throws Exception {
            final DefaultTempFile tempFile = new DefaultTempFile(this.tmpdir);
            this.tempFiles.add(tempFile);
            return tempFile;
        }

        @Override
        public void clear() {
            for (final TempFile file : this.tempFiles) {
                try {
                    file.delete();
                } catch (final Exception ignored) {
                }
            }
            this.tempFiles.clear();
        }
    }

    /**
     * Default strategy for creating and cleaning up temporary files.
     * <p/>
     * <p></p></[>By default, files are created by <code>File.createTempFile()</code> in
     * the directory specified.</p>
     */
    public static class DefaultTempFile implements TempFile {
        private final File file;
        private final OutputStream fstream;

        public DefaultTempFile(final String tempdir) throws IOException {
            this.file = File.createTempFile("NanoHTTPD-", "", new File(tempdir));
            this.fstream = new FileOutputStream(this.file);
        }

        @Override
        public OutputStream open() throws Exception {
            return this.fstream;
        }

        @Override
        public void delete() throws Exception {
            safeClose(this.fstream);
            this.file.delete();
        }

        @Override
        public String getName() {
            return this.file.getAbsolutePath();
        }
    }

    /**
     * HTTP response. Return one of these from serve().
     */
    public static class Response {
        /**
         * HTTP status code after processing, e.g. "200 OK", HTTP_OK
         */
        private IStatus status;
        /**
         * MIME type of content, e.g. "text/html"
         */
        private String mimeType;
        /**
         * Data of the response, may be null.
         */
        private InputStream data;
        /**
         * Headers for the HTTP response. Use addHeader() to add lines.
         */
        private final Map<String, String> header = new HashMap<String, String>();
        /**
         * The request method that spawned this response.
         */
        private Method requestMethod;
        /**
         * Use chunkedTransfer
         */
        private boolean chunkedTransfer;

        /**
         * Default constructor: response = HTTP_OK, mime = MIME_HTML and your supplied message
         */
        public Response(final String msg) {
            this(Status.OK, MIME_HTML, msg);
        }

        /**
         * Basic constructor.
         */
        public Response(final IStatus status, final String mimeType, final InputStream data) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = data;
        }

        /**
         * Convenience method that makes an InputStream out of given text.
         */
        public Response(final IStatus status, final String mimeType, final String txt) {
            this.status = status;
            this.mimeType = mimeType;
            try {
                this.data = txt != null ? new ByteArrayInputStream(txt.getBytes("UTF-8")) : null;
            } catch (final java.io.UnsupportedEncodingException uee) {
                uee.printStackTrace();
            }
        }

        /**
         * Adds given line to the header.
         */
        public void addHeader(final String name, final String value) {
            this.header.put(name, value);
        }

        public String getHeader(final String name) {
            return this.header.get(name);
        }

        /**
         * Sends given response to the socket.
         */
        protected void send(final OutputStream outputStream) {
            final String mime = this.mimeType;
            final SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

            try {
                if (this.status == null) {
                    throw new Error("sendResponse(): Status can't be null.");
                }
                final PrintWriter pw = new PrintWriter(outputStream);
                pw.print("HTTP/1.1 " + this.status.getDescription() + " \r\n");

                if (mime != null) {
                    pw.print("Content-Type: " + mime + "\r\n");
                }

                if ((this.header == null) || (this.header.get("Date") == null)) {
                    pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
                }

                if (this.header != null) {
                    for (final String key : this.header.keySet()) {
                        final String value = this.header.get(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }

                sendConnectionHeaderIfNotAlreadyPresent(pw, this.header);

                if ((this.requestMethod != Method.HEAD) && this.chunkedTransfer) {
                    sendAsChunked(outputStream, pw);
                } else {
                    final int pending = this.data != null ? this.data.available() : 0;
                    sendContentLengthHeaderIfNotAlreadyPresent(pw, this.header, pending);
                    pw.print("\r\n");
                    pw.flush();
                    sendAsFixedLength(outputStream, pending);
                }
                outputStream.flush();
                safeClose(this.data);
            } catch (final IOException ioe) {
                // Couldn't write? No can do.
            }
        }

        protected void sendContentLengthHeaderIfNotAlreadyPresent(final PrintWriter pw, final Map<String, String> header, final int size) {
            if (!headerAlreadySent(header, "content-length")) {
                pw.print("Content-Length: " + size + "\r\n");
            }
        }

        protected void sendConnectionHeaderIfNotAlreadyPresent(final PrintWriter pw, final Map<String, String> header) {
            if (!headerAlreadySent(header, "connection")) {
                pw.print("Connection: keep-alive\r\n");
            }
        }

        private boolean headerAlreadySent(final Map<String, String> header, final String name) {
            boolean alreadySent = false;
            for (final String headerName : header.keySet()) {
                alreadySent |= headerName.equalsIgnoreCase(name);
            }
            return alreadySent;
        }

        private void sendAsChunked(final OutputStream outputStream, final PrintWriter pw) throws IOException {
            pw.print("Transfer-Encoding: chunked\r\n");
            pw.print("\r\n");
            pw.flush();
            final int BUFFER_SIZE = 16 * 1024;
            final byte[] CRLF = "\r\n".getBytes();
            final byte[] buff = new byte[BUFFER_SIZE];
            int read;
            while ((read = this.data.read(buff)) > 0) {
                outputStream.write(String.format("%x\r\n", read).getBytes());
                outputStream.write(buff, 0, read);
                outputStream.write(CRLF);
            }
            outputStream.write(String.format("0\r\n\r\n").getBytes());
        }

        private void sendAsFixedLength(final OutputStream outputStream, int pending) throws IOException {
            if ((this.requestMethod != Method.HEAD) && (this.data != null)) {
                final int BUFFER_SIZE = 16 * 1024;
                final byte[] buff = new byte[BUFFER_SIZE];
                while (pending > 0) {
                    final int read = this.data.read(buff, 0, ((pending > BUFFER_SIZE) ? BUFFER_SIZE : pending));
                    if (read <= 0) {
                        break;
                    }
                    outputStream.write(buff, 0, read);
                    pending -= read;
                }
            }
        }

        public IStatus getStatus() {
            return this.status;
        }

        public void setStatus(final Status status) {
            this.status = status;
        }

        public String getMimeType() {
            return this.mimeType;
        }

        public void setMimeType(final String mimeType) {
            this.mimeType = mimeType;
        }

        public InputStream getData() {
            return this.data;
        }

        public void setData(final InputStream data) {
            this.data = data;
        }

        public Method getRequestMethod() {
            return this.requestMethod;
        }

        public void setRequestMethod(final Method requestMethod) {
            this.requestMethod = requestMethod;
        }

        public void setChunkedTransfer(final boolean chunkedTransfer) {
            this.chunkedTransfer = chunkedTransfer;
        }

        public interface IStatus {
            int getRequestStatus();

            String getDescription();
        }

        /**
         * Some HTTP response status codes
         */
        public enum Status implements IStatus {
            SWITCH_PROTOCOL(101, "Switching Protocols"),
            OK(200, "OK"),
            CREATED(201, "Created"),
            ACCEPTED(202, "Accepted"),
            NO_CONTENT(204, "No Content"),
            PARTIAL_CONTENT(206, "Partial Content"),
            REDIRECT(301, "Moved Permanently"),
            NOT_MODIFIED(304, "Not Modified"),
            BAD_REQUEST(400, "Bad Request"),
            UNAUTHORIZED(401, "Unauthorized"),
            FORBIDDEN(403, "Forbidden"),
            NOT_FOUND(404, "Not Found"),
            METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
            RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
            INTERNAL_ERROR(500, "Internal Server Error");
            private final int requestStatus;
            private final String description;

            Status(final int requestStatus, final String description) {
                this.requestStatus = requestStatus;
                this.description = description;
            }

            @Override
            public int getRequestStatus() {
                return this.requestStatus;
            }

            @Override
            public String getDescription() {
                return "" + this.requestStatus + " " + this.description;
            }
        }
    }

    public static final class ResponseException extends Exception {

        private final Response.Status status;

        public ResponseException(final Response.Status status, final String message) {
            super(message);
            this.status = status;
        }

        public ResponseException(final Response.Status status, final String message, final Exception e) {
            super(message, e);
            this.status = status;
        }

        public Response.Status getStatus() {
            return this.status;
        }
    }

    /**
     * Handles one session, i.e. parses the HTTP request and returns the response.
     */
    public interface IHTTPSession {
        void execute() throws IOException;

        Map<String, String> getParms();

        Map<String, String> getHeaders();

        /**
         * @return the path part of the URL.
         */
        String getUri();

        String getQueryParameterString();

        Method getMethod();

        InputStream getInputStream();

        CookieHandler getCookies();

        /**
         * Adds the files in the request body to the files map.
         * @return
         * @arg files - map to modify
         */
        boolean parseBody(Map<String, String> files) throws IOException, ResponseException;
    }

    protected class HTTPSession implements IHTTPSession {
        public static final int BUFSIZE = 8192;
        private final TempFileManager tempFileManager;
        private final OutputStream outputStream;
        private final PushbackInputStream inputStream;
        private int splitbyte;
        private int rlen;
        private String uri;
        private Method method;
        private Map<String, String> parms;
        private Map<String, String> headers;
        private CookieHandler cookies;
        private String queryParameterString;

        public HTTPSession(final TempFileManager tempFileManager, final InputStream inputStream, final OutputStream outputStream) {
            this.tempFileManager = tempFileManager;
            this.inputStream = new PushbackInputStream(inputStream, BUFSIZE);
            this.outputStream = outputStream;
        }

        public HTTPSession(final TempFileManager tempFileManager, final InputStream inputStream, final OutputStream outputStream, final InetAddress inetAddress) {
            this.tempFileManager = tempFileManager;
            this.inputStream = new PushbackInputStream(inputStream, BUFSIZE);
            this.outputStream = outputStream;
            final String remoteIp = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "127.0.0.1" : inetAddress.getHostAddress().toString();
            this.headers = new HashMap<String, String>();

            this.headers.put("remote-addr", remoteIp);
            this.headers.put("http-client-ip", remoteIp);
        }

        @Override
        public void execute() throws IOException {
            try {
                // Read the first 8192 bytes.
                // The full header should fit in here.
                // Apache's default header limit is 8KB.
                // Do NOT assume that a single read will get the entire header at once!
                final byte[] buf = new byte[BUFSIZE];
                this.splitbyte = 0;
                this.rlen = 0;
                {
                    int read = -1;
                    try {
                        read = this.inputStream.read(buf, 0, BUFSIZE);
                    } catch (final Exception e) {
                        safeClose(this.inputStream);
                        safeClose(this.outputStream);
                        throw new SocketException("NanoHttpd Shutdown");
                    }
                    if (read == -1) {
                        // socket was been closed
                        safeClose(this.inputStream);
                        safeClose(this.outputStream);
                        throw new SocketException("NanoHttpd Shutdown");
                    }
                    while (read > 0) {
                        this.rlen += read;
                        this.splitbyte = findHeaderEnd(buf, this.rlen);
                        if (this.splitbyte > 0) {
                            break;
                        }
                        read = this.inputStream.read(buf, this.rlen, BUFSIZE - this.rlen);
                    }
                }

                if (this.splitbyte < this.rlen) {
                    this.inputStream.unread(buf, this.splitbyte, this.rlen - this.splitbyte);
                }

                this.parms = new HashMap<String, String>();
                if (null == this.headers) {
                    this.headers = new HashMap<String, String>();
                }

                // Create a BufferedReader for parsing the header.
                final BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, this.rlen)));

                // Decode the header into parms and header java properties
                final Map<String, String> pre = new HashMap<String, String>();
                decodeHeader(hin, pre, this.parms, this.headers);

                this.method = Method.lookup(pre.get("method"));
                if (this.method == null) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error.");
                }

                this.uri = pre.get("uri");

                this.cookies = new CookieHandler(this.headers);

                // Ok, now do the serve()
                final Response r = serve(this);
                if (r == null) {
                    throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                } else {
                    this.cookies.unloadQueue(r);
                    r.setRequestMethod(this.method);
                    r.send(this.outputStream);
                }
            } catch (final SocketException e) {
                // throw it out to close socket object (finalAccept)
                throw e;
            } catch (final SocketTimeoutException ste) {
                throw ste;
            } catch (final IOException ioe) {
                final Response r = new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                r.send(this.outputStream);
                safeClose(this.outputStream);
            } catch (final ResponseException re) {
                final Response r = new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                r.send(this.outputStream);
                safeClose(this.outputStream);
            } finally {
                this.tempFileManager.clear();
            }
        }

        @Override
        public boolean parseBody(final Map<String, String> files) throws IOException, ResponseException {
            RandomAccessFile randomAccessFile = null;
            BufferedReader in = null;
            try {

                randomAccessFile = getTmpBucket();

                long size;
                if (this.headers.containsKey("content-length")) {
                    size = Integer.parseInt(this.headers.get("content-length"));
                } else if (this.splitbyte < this.rlen) {
                    size = this.rlen - this.splitbyte;
                } else {
                    size = 0;
                }

                if (size > Main.config().CONTENT.MAX_UPLOAD) {
                    return false;
                }

                // Now read all the body and write it to f
                final byte[] buf = new byte[512];
                while ((this.rlen >= 0) && (size > 0)) {
                    this.rlen = this.inputStream.read(buf, 0, (int) Math.min(size, 512));
                    size -= this.rlen;
                    if (this.rlen > 0) {
                        randomAccessFile.write(buf, 0, this.rlen);
                    }
                }

                // Get the raw body as a byte []
                final ByteBuffer fbuf = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length());
                randomAccessFile.seek(0);

                // Create a BufferedReader for easily reading it as string.
                final InputStream bin = new FileInputStream(randomAccessFile.getFD());
                in = new BufferedReader(new InputStreamReader(bin));

                // If the method is POST, there may be parameters
                // in data section, too, read it:
                if (Method.POST.equals(this.method)) {
                    String contentType = "";
                    final String contentTypeHeader = this.headers.get("content-type");

                    StringTokenizer st = null;
                    if (contentTypeHeader != null) {
                        st = new StringTokenizer(contentTypeHeader, ",; ");
                        if (st.hasMoreTokens()) {
                            contentType = st.nextToken();
                        }
                    }

                    if ("multipart/form-data".equalsIgnoreCase(contentType)) {
                        // Handle multipart/form-data
                        if (!st.hasMoreTokens()) {
                            throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html");
                        }

                        final String boundaryStartString = "boundary=";
                        final int boundaryContentStart = contentTypeHeader.indexOf(boundaryStartString) + boundaryStartString.length();
                        String boundary = contentTypeHeader.substring(boundaryContentStart, contentTypeHeader.length());
                        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                            boundary = boundary.substring(1, boundary.length() - 1);
                        }

                        decodeMultipartData(boundary, fbuf, in, this.parms, files);
                    } else {
                        String postLine = "";
                        final StringBuilder postLineBuffer = new StringBuilder();
                        final char pbuf[] = new char[512];
                        int read = in.read(pbuf);
                        while ((read >= 0) && !postLine.endsWith("\r\n")) {
                            postLine = String.valueOf(pbuf, 0, read);
                            postLineBuffer.append(postLine);
                            read = in.read(pbuf);
                        }
                        postLine = postLineBuffer.toString().trim();
                        // Handle application/x-www-form-urlencoded
                        if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
                            decodeParms(postLine, this.parms);
                        } else if (postLine.length() != 0) {
                            // Special case for raw POST data => create a special files entry "postData" with raw content data
                            files.put("postData", postLine);
                        }
                    }
                } else if (Method.PUT.equals(this.method)) {
                    files.put("content", saveTmpFile(fbuf, 0, fbuf.limit()));
                }
            } finally {
                safeClose(randomAccessFile);
                safeClose(in);
            }

            return true;
        }

        /**
         * Decodes the sent headers and loads the data into Key/value pairs
         */
        private void decodeHeader(final BufferedReader in, final Map<String, String> pre, final Map<String, String> parms, final Map<String, String> headers) throws ResponseException {
            try {
                // Read the request line
                final String inLine = in.readLine();
                if (inLine == null) {
                    return;
                }

                final StringTokenizer st = new StringTokenizer(inLine);
                if (!st.hasMoreTokens()) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                }

                pre.put("method", st.nextToken());

                if (!st.hasMoreTokens()) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
                }

                String uri = st.nextToken();

                // Decode parameters from the URI
                final int qmi = uri.indexOf('?');
                if (qmi >= 0) {
                    decodeParms(uri.substring(qmi + 1), parms);
                    uri = decodePercent(uri.substring(0, qmi));
                } else {
                    uri = decodePercent(uri);
                }

                // If there's another token, it's protocol version,
                // followed by HTTP headers. Ignore version but parse headers.
                // NOTE: this now forces header names lowercase since they are
                // case insensitive and vary by client.
                if (st.hasMoreTokens()) {
                    String line = in.readLine();
                    while ((line != null) && (line.trim().length() > 0)) {
                        final int p = line.indexOf(':');
                        if (p >= 0) {
                            headers.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1).trim());
                        }
                        line = in.readLine();
                    }
                }

                pre.put("uri", uri);
            } catch (final IOException ioe) {
                throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
            }
        }

        /**
         * Decodes the Multipart Body data and put it into Key/Value pairs.
         */
        private void decodeMultipartData(final String boundary, final ByteBuffer fbuf, final BufferedReader in, final Map<String, String> parms, final Map<String, String> files) throws ResponseException {
            try {
                final int[] bpositions = getBoundaryPositions(fbuf, boundary.getBytes());
                int boundarycount = 1;
                String mpline = in.readLine();
                while (mpline != null) {
                    if (!mpline.contains(boundary)) {
                        throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but next chunk does not start with boundary. Usage: GET /example/file.html");
                    }
                    boundarycount++;
                    final Map<String, String> item = new HashMap<String, String>();
                    mpline = in.readLine();
                    while ((mpline != null) && (mpline.trim().length() > 0)) {
                        final int p = mpline.indexOf(':');
                        if (p != -1) {
                            item.put(mpline.substring(0, p).trim().toLowerCase(Locale.US), mpline.substring(p + 1).trim());
                        }
                        mpline = in.readLine();
                    }
                    if (mpline != null) {
                        final String contentDisposition = item.get("content-disposition");
                        if (contentDisposition == null) {
                            throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but no content-disposition info found. Usage: GET /example/file.html");
                        }
                        final StringTokenizer st = new StringTokenizer(contentDisposition, ";");
                        final Map<String, String> disposition = new HashMap<String, String>();
                        while (st.hasMoreTokens()) {
                            final String token = st.nextToken().trim();
                            final int p = token.indexOf('=');
                            if (p != -1) {
                                disposition.put(token.substring(0, p).trim().toLowerCase(Locale.US), token.substring(p + 1).trim());
                            }
                        }
                        String pname = disposition.get("filename");
                        if (pname == null) {
                            pname = disposition.get("name");
                        }
                        pname = pname.substring(1, pname.length() - 1);
                        String value = "";
                        if (item.get("content-type") == null) {
                            while ((mpline != null) && !mpline.contains(boundary)) {
                                mpline = in.readLine();
                                if (mpline != null) {
                                    final int d = mpline.indexOf(boundary);
                                    if (d == -1) {
                                        value += mpline;
                                    } else {
                                        value += mpline.substring(0, d - 2);
                                    }
                                }
                            }
                        } else {
                            if (boundarycount > bpositions.length) {
                                throw new ResponseException(Response.Status.INTERNAL_ERROR, "Error processing request");
                            }
                            final int offset = stripMultipartHeaders(fbuf, bpositions[boundarycount - 2]);
                            final String path = saveTmpFile(fbuf, offset, bpositions[boundarycount - 1] - offset - 4);
                            files.put(pname, path);
                            value = disposition.get("filename");
                            value = value.substring(1, value.length() - 1);
                            do {
                                mpline = in.readLine();
                            } while ((mpline != null) && !mpline.contains(boundary));
                        }
                        parms.put(pname, value);
                    }
                }
            } catch (final IOException ioe) {
                throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
            }
        }

        /**
         * Find byte index separating header from body. It must be the last byte of the first two sequential new lines.
         */
        private int findHeaderEnd(final byte[] buf, final int rlen) {
            int splitbyte = 0;
            while ((splitbyte + 3) < rlen) {
                if ((buf[splitbyte] == '\r') && (buf[splitbyte + 1] == '\n') && (buf[splitbyte + 2] == '\r') && (buf[splitbyte + 3] == '\n')) {
                    return splitbyte + 4;
                }
                splitbyte++;
            }
            return 0;
        }

        /**
         * Find the byte positions where multipart boundaries start.
         */
        private int[] getBoundaryPositions(final ByteBuffer b, final byte[] boundary) {
            int matchcount = 0;
            int matchbyte = -1;
            final List<Integer> matchbytes = new ArrayList<Integer>();
            for (int i = 0; i < b.limit(); i++) {
                if (b.get(i) == boundary[matchcount]) {
                    if (matchcount == 0) {
                        matchbyte = i;
                    }
                    matchcount++;
                    if (matchcount == boundary.length) {
                        matchbytes.add(matchbyte);
                        matchcount = 0;
                        matchbyte = -1;
                    }
                } else {
                    i -= matchcount;
                    matchcount = 0;
                    matchbyte = -1;
                }
            }
            final int[] ret = new int[matchbytes.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = matchbytes.get(i);
            }
            return ret;
        }

        /**
         * Retrieves the content of a sent file and saves it to a temporary file. The full path to the saved file is returned.
         */
        private String saveTmpFile(final ByteBuffer b, final int offset, final int len) {
            String path = "";
            if (len > 0) {
                FileOutputStream fileOutputStream = null;
                try {
                    final TempFile tempFile = this.tempFileManager.createTempFile();
                    final ByteBuffer src = b.duplicate();
                    fileOutputStream = new FileOutputStream(tempFile.getName());
                    final FileChannel dest = fileOutputStream.getChannel();
                    src.position(offset).limit(offset + len);
                    dest.write(src.slice());
                    path = tempFile.getName();
                } catch (final Exception e) { // Catch exception if any
                    throw new Error(e); // we won't recover, so throw an error
                } finally {
                    safeClose(fileOutputStream);
                }
            }
            return path;
        }

        private RandomAccessFile getTmpBucket() {
            try {
                final TempFile tempFile = this.tempFileManager.createTempFile();
                return new RandomAccessFile(tempFile.getName(), "rw");
            } catch (final Exception e) {
                throw new Error(e); // we won't recover, so throw an error
            }
        }

        /**
         * It returns the offset separating multipart file headers from the file's data.
         */
        private int stripMultipartHeaders(final ByteBuffer b, final int offset) {
            int i;
            for (i = offset; i < b.limit(); i++) {
                if ((b.get(i) == '\r') && (b.get(++i) == '\n') && (b.get(++i) == '\r') && (b.get(++i) == '\n')) {
                    break;
                }
            }
            return i + 1;
        }

        /**
         * Decodes parameters in percent-encoded URI-format ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and
         * adds them to given Map. NOTE: this doesn't support multiple identical keys due to the simplicity of Map.
         */
        private void decodeParms(final String parms, final Map<String, String> p) {
            if (parms == null) {
                this.queryParameterString = "";
                return;
            }

            this.queryParameterString = parms;
            final StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens()) {
                final String e = st.nextToken();
                final int sep = e.indexOf('=');
                if (sep >= 0) {
                    p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
                } else {
                    p.put(decodePercent(e).trim(), "");
                }
            }
        }

        @Override
        public final Map<String, String> getParms() {
            return this.parms;
        }

        @Override
        public String getQueryParameterString() {
            return this.queryParameterString;
        }

        @Override
        public final Map<String, String> getHeaders() {
            return this.headers;
        }

        @Override
        public final String getUri() {
            return this.uri;
        }

        @Override
        public final Method getMethod() {
            return this.method;
        }

        @Override
        public final InputStream getInputStream() {
            return this.inputStream;
        }

        @Override
        public CookieHandler getCookies() {
            return this.cookies;
        }
    }

    public static class Cookie {
        private final String n, v, e;

        public Cookie(final String name, final String value, final String expires) {
            this.n = name;
            this.v = value;
            this.e = expires;
        }

        public Cookie(final String name, final String value) {
            this(name, value, 30);
        }

        public Cookie(final String name, final String value, final int numDays) {
            this.n = name;
            this.v = value;
            this.e = getHTTPTime(numDays);
        }

        public String getHTTPHeader() {
            final String fmt = "%s=%s; expires=%s";
            return String.format(fmt, this.n, this.v, this.e);
        }

        public static String getHTTPTime(final int days) {
            final Calendar calendar = Calendar.getInstance();
            final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            calendar.add(Calendar.DAY_OF_MONTH, days);
            return dateFormat.format(calendar.getTime());
        }
    }

    /**
     * Provides rudimentary support for cookies.
     * Doesn't support 'path', 'secure' nor 'httpOnly'.
     * Feel free to improve it and/or add unsupported features.
     *
     * @author LordFokas
     */
    public class CookieHandler implements Iterable<String> {
        private final HashMap<String, String> cookies = new HashMap<String, String>();
        private final ArrayList<Cookie> queue = new ArrayList<Cookie>();

        public CookieHandler(final Map<String, String> httpHeaders) {
            final String raw = httpHeaders.get("cookie");
            if (raw != null) {
                final String[] tokens = raw.split(";");
                for (final String token : tokens) {
                    final String[] data = token.trim().split("=");
                    if (data.length == 2) {
                        this.cookies.put(data[0], data[1]);
                    }
                }
            }
        }

        @Override
        public Iterator<String> iterator() {
            return this.cookies.keySet().iterator();
        }

        /**
         * Read a cookie from the HTTP Headers.
         *
         * @param name The cookie's name.
         * @return The cookie's value if it exists, null otherwise.
         */
        public String read(final String name) {
            return this.cookies.get(name);
        }

        /**
         * Sets a cookie.
         *
         * @param name    The cookie's name.
         * @param value   The cookie's value.
         * @param expires How many days until the cookie expires.
         */
        public void set(final String name, final String value, final int expires) {
            this.queue.add(new Cookie(name, value, Cookie.getHTTPTime(expires)));
        }

        public void set(final Cookie cookie) {
            this.queue.add(cookie);
        }

        /**
         * Set a cookie with an expiration date from a month ago, effectively deleting it on the client side.
         *
         * @param name The cookie name.
         */
        public void delete(final String name) {
            set(name, "-delete-", -30);
        }

        /**
         * Internally used by the webserver to add all queued cookies into the Response's HTTP Headers.
         *
         * @param response The Response object to which headers the queued cookies will be added.
         */
        public void unloadQueue(final Response response) {
            for (final Cookie cookie : this.queue) {
                response.addHeader("Set-Cookie", cookie.getHTTPHeader());
            }
        }

        public HashMap<String, String> getCookies() {
            return cookies;
        }
    }
}
