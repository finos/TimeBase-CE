package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Provides an {@link OutputStream} connected to {@link InputStream} with intermediate buffer.
 * Mainly used for communication between threads.
 *
 * @author Alexei Osipov
 */
public class CommunicationPipe {
    private final PipedOutputStream outputStream;
    private final PipedInputStream inputStream;

    public CommunicationPipe() {
        this(8 * 1024);
    }

    public CommunicationPipe(int bufferSize) {
        this.outputStream = new PipedOutputStream();
        try {
            this.inputStream = new PipedInputStream(this.outputStream, bufferSize);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
