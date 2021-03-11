package com.epam.deltix.qsrv.hf.topic.loader;

import com.google.common.annotations.VisibleForTesting;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * {@link MessageChannel} that accepts {@link MemoryDataOutput} as input and writes it's content
 * to a provided {@link OutputStream}.
 *
 * @author Alexei Osipov
 */
@VisibleForTesting
public class MemoryDataOutputStreamChannel implements MessageChannel<MemoryDataOutput> {
    private final DataOutputStream outputStream;

    public MemoryDataOutputStreamChannel(OutputStream outputStream) {
        this.outputStream = new DataOutputStream(outputStream);
    }

    @Override
    public void send(MemoryDataOutput msg) {
        try {
            int length = msg.getPosition();
            outputStream.writeInt(length);
            outputStream.write(msg.getBuffer(), 0, length);
            outputStream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        // TODO: Review
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
