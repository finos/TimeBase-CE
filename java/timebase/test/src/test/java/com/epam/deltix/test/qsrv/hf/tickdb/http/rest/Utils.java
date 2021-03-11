package com.epam.deltix.test.qsrv.hf.tickdb.http.rest;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.pub.md.UHFJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol;
import com.epam.deltix.qsrv.hf.tickdb.http.TBJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.GetSchemaRequest;
import com.epam.deltix.util.codec.Base64EncoderEx;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

/**
 *
 */
public class Utils {
    public static MessageChannel<RawMessage> getRestLoader(String host, int port,
                                                            boolean useCompression, boolean isBigEndian, String streamName,
                                                            RecordClassDescriptor[] rcds, String user, String password) throws IOException {
        Socket socket = connectAndHandshake(host, port, user, password);
        return new RESTLoader(socket, useCompression, isBigEndian, streamName, rcds);
    }

    public static Socket connectAndHandshake(String host, int port, String user, String password) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(0);
        socket.connect(new InetSocketAddress(host, port), 5000);

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        DataInputStream dis = new DataInputStream(socket.getInputStream());

        dos.write(HTTPProtocol.PROTOCOL_INIT);
        short version = dis.readShort();
        dos.writeShort(HTTPProtocol.VERSION);
        HTTPProtocol.validateVersion(version);

        dos.writeBoolean(user != null);
        if (user != null) {
            dos.writeUTF(user);
            dos.writeUTF(Base64EncoderEx.encode(password.getBytes()));
        }
        dos.flush();

        int handshake = dis.readInt();
        if (handshake == HTTPProtocol.RESP_ERROR) {
            String className = dis.readUTF();
            String msg = dis.readUTF();
            throw new RuntimeException("Handshake Error: " + className + ". Msg: " + msg);
        }

        return socket;
    }

    public static RecordClassSet requestSchema(URL url, String streamName, String user, String password) throws IOException, JAXBException {
        GetSchemaRequest r = new GetSchemaRequest();
        r.stream = streamName;
        Marshaller m = TBJAXBContext.createMarshaller();

        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (user != null) {
            String authStr = user + ":" + password;
            final String encodedAuth = Base64EncoderEx.encode(authStr.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();

        m.marshal(r, os);
        int rc = conn.getResponseCode();
        if (rc != 200) {
            throw new RuntimeException("HTTP rc=" + rc + " " + conn.getResponseMessage());
        }

        InputStream is = conn.getInputStream();
        Unmarshaller u = UHFJAXBContext.createUnmarshaller();
        return (RecordClassSet) u.unmarshal(is);
    }
}
