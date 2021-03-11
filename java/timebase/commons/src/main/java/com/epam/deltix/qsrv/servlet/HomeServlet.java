package com.epam.deltix.qsrv.servlet;

import com.epam.deltix.qsrv.QSHome;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class HomeServlet extends HttpServlet {

    // HomeServlet is never protected by SSL or UAC

    @Override
    protected void      service (HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        resp.setContentType("text/plain");
        DataOutputStream dout = new DataOutputStream(resp.getOutputStream());
        dout.writeUTF(QSHome.get());
        dout.flush();
    }

    public static String get(String host, int port) throws IOException {
        URLConnection connection =
                new URL("http", host, port, "/gethome").openConnection();
        connection.setReadTimeout(5000);

        InputStream in = connection.getInputStream();
        String value = read(in);
        in.close();

        return value;
    }

    public static String read(InputStream in) throws IOException {
        return new DataInputStream(in).readUTF();
    }
}