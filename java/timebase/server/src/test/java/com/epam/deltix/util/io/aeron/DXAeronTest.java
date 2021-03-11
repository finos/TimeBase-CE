package com.epam.deltix.util.io.aeron;

import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXAeronHelper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Alexei Osipov
 */
public class DXAeronTest {
    @Test
    public void startClient() throws Exception {
        DXAeronHelper.start(false);
        InputStream in = DXAeron.createInputStream(42);
        int size = 128 * 1024;
        byte[] buffer = new byte[size];
        long totalRead = 0;
        PrintingCounter watch = new PrintingCounter("Bytes read");
        watch.start();
        while (true) {
            int read = in.read(buffer);
            for (int i = 0; i < read; i++) {
                if (buffer[i] != (byte)((totalRead + i) % size)) {
                    throw new IllegalStateException();
                }
            }
            totalRead += read;
            watch.add(read);
        }
        //watch.stop();
    }

    public static void main(String[] args) throws IOException {
        startServer();
    }

    private static void startServer() throws IOException {
        DXAeronHelper.start(true);
        OutputStream out = DXAeron.createOutputStream(42);
        int size = 128 * 1024;
        byte[] buffer = new byte[size];
        for (int i = 0; i < size; i++) {
            buffer[i] = (byte)i;
        }
        while (true) {
            out.write(buffer);
            out.flush();
        }
    }
}