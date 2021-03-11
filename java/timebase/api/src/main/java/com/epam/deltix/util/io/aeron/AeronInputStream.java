package com.epam.deltix.util.io.aeron;

import com.epam.deltix.util.lang.Util;
import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;

import java.io.IOException;
import java.io.InputStream;

import static com.epam.deltix.util.vsocket.VSProtocol.CHANNEL_MAX_BUFFER_SIZE;

@Deprecated
public class AeronInputStream extends InputStream {
    private final IdleStrategy          idleStrategy;
    protected final Subscription        subscription;
    protected final FragmentAssembler   subscribeHandler;

    protected byte[]                    internalBuf = new byte[CHANNEL_MAX_BUFFER_SIZE << 2];
    protected int                       dataOffset = 0;
    protected int                       dataSize = 0;

    AeronInputStream(Aeron aeron, String channel, int streamId) {
        this(aeron, channel, streamId,
            new NoOpIdleStrategy());
    }

    AeronInputStream(Aeron aeron, String channel, int streamId, IdleStrategy idleStrategy) {
        this.idleStrategy = idleStrategy;
        subscription = aeron.addSubscription(channel, streamId);

        subscribeHandler = new FragmentAssembler(createSubscribeHandler());
    }

    protected FragmentHandler           createSubscribeHandler() {
        return (buffer, offset, length, header) -> {
            putBytes(buffer, offset, length);
        };
    }

    @Override
    public int                          read() throws IOException {
        if (getDataSize() <= 0)
            receiveBytes();

        return getByte();
    }

    @Override
    public int                          read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (getDataSize() <= 0)
            receiveBytes();
        return getBytes(b, off, len);
    }

    private void                        receiveBytes() throws IOException {
        int fragmentsRead;
        while ((fragmentsRead = subscription.poll(subscribeHandler, Integer.MAX_VALUE)) == 0) {
            idleStrategy.idle(fragmentsRead);

            if (subscription.isClosed())
                throw new IOException("Stream is closed!");
        }
    }

    protected synchronized void         putBytes(DirectBuffer buffer, int offset, int length) {
        if (dataOffset + dataSize + length > internalBuf.length) {
            shiftBuffer();
            if (dataOffset + dataSize + length > internalBuf.length)
                extendBuffer(dataOffset + dataSize + length);
        }

        buffer.getBytes(offset, internalBuf, dataOffset + dataSize, length);
        dataSize += length;
    }

    protected synchronized int          getByte() {
        int res = internalBuf[dataOffset] & 0xFF;
        dataSize -= 1;
        dataOffset += 1;

        return res;
    }

    protected synchronized int          getBytes(byte[] b, int offset, int length) {
        int count = Math.min(dataSize, length);

        System.arraycopy(internalBuf, this.dataOffset, b, offset, count);
        dataSize -= count;
        this.dataOffset += count;

        return count;
    }

    protected synchronized int          getDataSize() {
        return dataSize;
    }

    private void                        shiftBuffer() {
        System.arraycopy(internalBuf, dataOffset, internalBuf, 0, dataSize);
        dataOffset = 0;
    }

    private void                        extendBuffer(int newSize) {
        int newBufSize = Util.doubleUntilAtLeast(internalBuf.length, newSize);

        System.out.println("Buffer allocation: " + newBufSize);

        byte[] newBuffer = new byte[newBufSize];
        System.arraycopy(internalBuf, dataOffset, newBuffer, 0, dataSize);
        internalBuf = newBuffer;
        dataOffset = 0;
    }

    @Override
    public synchronized void            close() {
        if (subscription != null) {
            subscription.close();
        }
    }
}
