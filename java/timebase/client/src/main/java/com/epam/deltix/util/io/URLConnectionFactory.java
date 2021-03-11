package com.epam.deltix.util.io;

import com.epam.deltix.util.net.NetUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class URLConnectionFactory {

    /*
        Create new URL connection from given url.
     */
    public static URLConnection create(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setReadTimeout(5000);

        if (connection instanceof HttpsURLConnection)
            ((HttpsURLConnection) connection).setSSLSocketFactory(SSLClientContextProvider.getSSLContext().getSocketFactory());

        return connection;
    }

    public static URLConnection verify(URLConnection connection, String user, String pass) throws IOException {
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) connection;

            if (http.getResponseCode() == 302) { // redirect
                URLConnection redirect = create(new URL(http.getHeaderField("Location")));
                if (user != null && pass != null)
                    NetUtils.INSTANCE.authorize(redirect, user, pass);

                return redirect;
            }
        }

        return connection;
    }

    /*
        Create new URL connection.
     */
    public static URLConnection create(String host, int port, String file, boolean secured) throws IOException {
        URLConnection connection = new URL(getHttpProtocol(secured), host, port, file).openConnection();
        connection.setReadTimeout(5000);

        if (connection instanceof HttpsURLConnection)
            ((HttpsURLConnection) connection).setSSLSocketFactory(SSLClientContextProvider.getSSLContext().getSocketFactory());

        return connection;
    }

    public static String                getHttpProtocol(boolean ssl) {
        return ssl ? "https" : "http";
    }
}
