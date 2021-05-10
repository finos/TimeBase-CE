package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.UserDBClient;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.net.NetUtils;
import com.epam.deltix.util.time.TimeKeeper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;

/**
 *  Public methods for creating instances of {@link TickDB}.
 */
public class TickDBFactory {
    private static final Log LOG = LogFactory.getLog(TickDBFactory.class);
    //public static final String  CATALOG_NAME = "dbcat.txt";
    public static final String  EVENTS_STREAM_NAME = "events#";
    
    private static final String HOME_MAGIC = "${home}";

    private static final String DEFAULT_URL               = "http://gw.deltixlab.com/";
    private static final String SECONDARY_URL             = "http://ls.deltixlab.com/";

    public static String            VERSION_PROPERTY          = "TimeBase.version";

    private static String           defaultApplicationID;

    private static final String     defaultImplementationClass = "com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl";
    
    /**
     *  Creates a new embedded database with the specified root folders.
     *
     *  @param paths    All root folders comprising the database.
     *  @return         An un-opened TickDB instance. Next, the caller should
     *                  call {@link TickDB#open} or {@link TickDB#format}.
     *
     */
    public static DXTickDB       create (File ... paths) {
        try {
            Class<?> impl = TickDBFactory.class.getClassLoader().loadClass(defaultImplementationClass); // using runtime class loader
            Constructor constructor = impl.getConstructor(File[].class);
            return (DXTickDB) constructor.newInstance((Object) paths);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //return (new com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl(paths));
    }

    /**
     *  Creates a new embedded database with the specified root folders.
     *
     *  @param paths    All root folders comprising the database.
     *  @param options  Options for data caching.
     * 
     *  @return         An un-opened TickDB instance. Next, the caller should
     *                  call {@link TickDB#open} or {@link TickDB#format}.
     */
    public static DXTickDB       create (DataCacheOptions options, File ... paths) {

        try {
            Class<?> impl = TickDBFactory.class.getClassLoader().loadClass(defaultImplementationClass); // using runtime class loader
            Constructor constructor = impl.getConstructor(DataCacheOptions.class, File[].class);
            return (DXTickDB) constructor.newInstance(options, (Object) paths);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //return (new com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl(options, paths));
    }

    @Deprecated // use version that supports SSL and authentication
    public static DXTickDB      connect (String host, int port) {
        boolean                     isLocal = false;

        try {
            isLocal = InetAddress.getByName (host).isLoopbackAddress ();
        } catch (UnknownHostException x) {
            LOG.warn("Host '%s' is currently unknown.").with(host);
        }

        if (isLocal) {
            DXTickDB    reg = TimeBaseServerRegistry.getDirectConnection (port);

            if (reg != null)
                return (reg);
        }

        TickDBClient client = new TickDBClient(host, port);
        if (defaultApplicationID != null)
            client.setApplicationId(defaultApplicationID);

        return client;
    }

    public static RemoteTickDB  connect (String host, int port, boolean enableSSL) {
        return connect(host, port, enableSSL, null, null);
    }

    public static RemoteTickDB  connect (String host, int port, boolean enableSSL, String user, String pass) {
        TickDBClient client;

        if (user == null)
            client = new TickDBClient(host, port, enableSSL);
        else
            client = new TickDBClient(host, port, enableSSL, user, pass);

        if (defaultApplicationID != null)
            client.setApplicationId(defaultApplicationID);

        return client;
    }

    public static boolean       isRemote(String url) {
        return url.startsWith(TDBProtocol.PROTOCOL_PREFIX) || url.startsWith(TDBProtocol.SSL_PROTOCOL_PREFIX);
    }

    /**
     *  Creates a new database instance with the specified root folder, or URL.
     *
     *  @param url      Local folder or connection URL.
     *  @return         An un-opened TickDB instance. Next, the caller should
     *                  call {@link TickDB#open} or {@link TickDB#format}.
     */

    public static DXTickDB       createFromUrl (String url) {
        return createFromUrl(url, null, null);
    }

    /**
     *  Creates a new database instance with the specified URL and credentials
     *
     *  @param url      Local folder or connection URL.
     *  @return         An un-opened TickDB instance. Next, the caller should
     *                  call {@link TickDB#open} or {@link TickDB#format}.
     */

    public static DXTickDB       createFromUrl (String url, String user, String pass) {

        // dxtick://<user>:<pass> at <host>:<port>

        if (url.startsWith(TDBProtocol.PROTOCOL_PREFIX) ||
            url.startsWith(TDBProtocol.SSL_PROTOCOL_PREFIX)) {
            int         port;
            String      host;
            int         at = url.indexOf("@");

            //calc prefix length
            int prefixLength = TDBProtocol.PROTOCOL_PREFIX.length();

            if (url.startsWith(TDBProtocol.PROTOCOL_PREFIX))
                prefixLength = TDBProtocol.PROTOCOL_PREFIX.length();
            else if (url.startsWith(TDBProtocol.SSL_PROTOCOL_PREFIX))
                prefixLength = TDBProtocol.SSL_PROTOCOL_PREFIX.length();

            try {
                String      s = at != -1 ?
                        url.substring (at + 1) :
                        url.substring(prefixLength, url.length());

                int         colon = s.indexOf (':');

                if (colon < 0) {
                    host = s;
                    port = TDBProtocol.DEFAULT_PORT;
                }
                else {
                    host = s.substring (0, colon);
                    port = Integer.parseInt (s.substring (colon + 1));
                }

                if (at != -1) {
                    String[] login = url.substring (prefixLength, at).split(":");
                    if (user == null)
                        user = NetUtils.INSTANCE.decodeUrl(login[0]);

                    if (pass == null)
                        pass = login.length > 1 ? NetUtils.INSTANCE.decodeUrl(login[1]) : "";
                }
            } catch (Exception e) {
                throw new RuntimeException("Cannot parse Timebase connection URL: \"" + url +"\": " + e.getMessage(), e);
            }


            boolean enableSSL = url.startsWith(TDBProtocol.SSL_PROTOCOL_PREFIX);
            boolean isLocal = false;

            try {
                isLocal = InetAddress.getByName (host).isLoopbackAddress ();
            } catch (UnknownHostException x) {
                LOG.warn("Host '%s' is currently unknown.").with(host);
            }

            if (isLocal) {
                DXTickDB    reg = TimeBaseServerRegistry.getDirectConnection (port);

                if (reg != null)
                    return (reg);
            }

            return connect(host, port, enableSSL, user, pass);
        }
        else
            return (create (url));
    }

    /**
     * Formats Timebase url connection string using given credentials.
     * if user is not specified, then result will not contains any credentials
     */
    public static String       format (String url, String user, String pass) {

        // dxtick://usr:psw@<host>:<port>

        if (url.startsWith(TDBProtocol.PROTOCOL_PREFIX) ||
                url.startsWith(TDBProtocol.SSL_PROTOCOL_PREFIX)) {
            int         port;
            String      host;
            int         at = url.indexOf("@");

            //calc prefix length
            int prefixLength = TDBProtocol.PROTOCOL_PREFIX.length();

            if (url.startsWith(TDBProtocol.PROTOCOL_PREFIX))
                prefixLength = TDBProtocol.PROTOCOL_PREFIX.length();
            else if (url.startsWith(TDBProtocol.SSL_PROTOCOL_PREFIX))
                prefixLength = TDBProtocol.SSL_PROTOCOL_PREFIX.length();

            try {
                String      s = at != -1 ?
                        url.substring (at + 1) :
                        url.substring(prefixLength, url.length());

                int         colon = s.indexOf (':');

                if (colon < 0) {
                    host = s;
                    port = TDBProtocol.DEFAULT_PORT;
                }
                else {
                    host = s.substring (0, colon);
                    port = Integer.parseInt (s.substring (colon + 1));
                }

//                if (at != -1) {
//                    String[] login = url.substring (prefixLength, at).split(":");
//                    if (user == null)
//                        user = NetUtils.INSTANCE.decodeUrl(login[0]);
//
//                    if (pass == null)
//                        pass = login.length > 1 ? NetUtils.INSTANCE.decodeUrl(login[1]) : "";
//                }
            } catch (Exception e) {
                throw new RuntimeException("Cannot parse Timebase connection URL: \"" + url +"\": " + e.getMessage(), e);
            }

            boolean enableSSL = url.startsWith(TDBProtocol.SSL_PROTOCOL_PREFIX);

            return NetUtils.INSTANCE.formatUrl(enableSSL ? TDBProtocol.SSL_PROTOCOL_ID : TDBProtocol.PROTOCOL_ID, host, port, null, user, pass);
        }

        return url;
    }

//    private static DXTickDB       createFromChoice (String uri) {
//
//        ArrayList<String> content = new ArrayList<>();
//
//        HttpURLConnection c = null;
//
//        try {
//            try {
//                c = (HttpURLConnection) new URL(DEFAULT_URL + uri).openConnection();
//                c.connect();
//            } catch (IOException e) {
//                c = (HttpURLConnection) new URL(SECONDARY_URL + uri).openConnection();
//                c.connect();
//            }
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null)
//                content.add(line);
//            return TDBSelector.select(content.toArray(new String[content.size()]));
//
//        } catch (Exception e) {
//            if (c != null)
//                c.disconnect();
//
//            throw new com.epam.deltix.util.io.UncheckedIOException(e);
//        }
//    }

    private static File []      pathsToFiles (String [] paths) {
        File [] files = new File [paths.length];

        for (int ii = 0; ii < paths.length; ii++) {
            String      path = paths [ii];
            
            if (path.startsWith (HOME_MAGIC))
                path = Home.get () + path.substring (HOME_MAGIC.length ());
            
            files [ii] = new File (path);
        }

        return (files);
    }
    
    /**
     *  Creates a new embedded database with the specified root folders.
     *
     *  @param paths    All root folders comprising the database.
     * 
     *  @return         An un-opened TickDB instance. Next, the caller should
     *                  call {@link TickDB#open} or {@link TickDB#format}.
     */
    public static DXTickDB       create (String ... paths) {
        return (create (pathsToFiles (paths)));
    }

    /**
     *  Creates a new embedded database with the specified root folders.
     *
     *  @param paths    All root folders comprising the database.
     *  @param options  Options for data caching.
     * 
     *  @return         An un-opened TickDB instance. Next, the caller should
     *                  call {@link TickDB#open} or {@link TickDB#format}.
     */
    public static DXTickDB       create (DataCacheOptions options, String ... paths) {        
        return (create (options, pathsToFiles (paths)));
    }

    @Deprecated // use version that supports SSL and authentication
    public static DXTickDB          open (String host, int port, boolean readOnly) {
        try {
            DXTickDB result = connect (host, port);
            result.open(readOnly);
            return result;
        } catch (Throwable e) {
            LOG.warn("Unable to open %s:%s: %s").with(host).with(port).with(e);
            throw new RuntimeException("Unable to establish Timebase connection: " + host + ':' + port); // suppressing nested exception to (otherwise Spring unwraps it)
        }
    }

    /**
     * Creates and opens Timebase instance using specified url.
     * @param url timebase connection url
     * @param readOnly readonly connection
     * @return opened DXTickDB instance
     *
     * @throws  RuntimeException when connection cannot be established.
     */
    public static DXTickDB          openFromUrl(String url, boolean readOnly) {
        try {
            DXTickDB result = createFromUrl(url);
            result.open(readOnly);
            return result;
        } catch (Throwable e) {
            LOG.warn("Unable to open %s: %s").with(url).with(e);
            throw new RuntimeException("Unable to establish Timebase connection: " + url); // suppressing nested exception to (otherwise Spring unwraps it)
        }
    }

    /**
     * Creates and opens Timebase instance using specified url.
     * After given timeout throws exception if connection cannot be established.
     *
     * @param url timebase connection url
     * @param readOnly is readonly
     * @param timeout timeout to wait until connection is established
     * @return opened DXTickDB instance
     *
     * @throws  RuntimeException when connection cannot be established.
     */
    public static DXTickDB          openFromUrl(String url, boolean readOnly, long timeout)  {
        DXTickDB tdb = createFromUrl(url);

        long limit = TimeKeeper.currentTime + timeout;

        for (;;) {
            try {
                tdb.open(readOnly);
                return tdb;
            } catch (Throwable e) {
                if (TimeKeeper.currentTime > limit) {
                    LOG.warn("Unable to open %s: %s").with(url).with(e);
                    throw new RuntimeException("Unable to establish Timebase connection: " + url); // suppressing nested exception to (otherwise Spring unwraps it)
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                    }
                }
            }
        }
    }

    public static DXTickDB           wrap(DXTickDB timebase, Principal user) {
        if (timebase instanceof TickDBClient)
            return new UserDBClient((TickDBClient)timebase, user);

        throw new IllegalArgumentException("Wrapping " + timebase + " is not supported");
    }

    /**
     *  Set "Application Name" for the DXTickDB instance. Applicable for remote connections only.
     *  Should be used before @see deltix.qsrv.hf.tickdb.pub.DXTickDB#open(boolean)
     *  @param db DXTickDB instance
     *  @param application Name of the application that owns connection
     *
     */
    public static void      setApplicationName(DXTickDB db, String application) {
        if (db instanceof TickDBClient)
            ((TickDBClient) db).setApplicationId(application);
    }

    /**
     *  Set default "Application Name" that will be used for newly created connections.
     *  Specific application name can for single connection can be set
     *  using @see TickDBFactory#setApplicationName(DXTickDB, String)
     */
    public static void      setApplicationName(String application) {
        defaultApplicationID = application;
    }

}
