package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.util.concurrent.NextResult;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;

/**
 *
 */
public abstract class MessageQueueReader <T extends MessageQueue>
    extends RawReaderBase <T>
{
    private static final int                BUFFER_SIZE =
            Util.getIntSystemProperty("TimeBase.queueReaderBufferSize", 8192, 256, Integer.MAX_VALUE);

    private boolean                     isPolymorphic;
    private QuickMessageFilter          filter = null;
    private final StringBuilder         symbol = new StringBuilder ();

    protected long                      bufferFileOffset = 0;
    protected int                       bufferPosition = 0;

    protected boolean                   realTimeNotifications = false;
    boolean                             isRealTime = false;

    protected MessageQueueReader(TickStreamImpl stream) {
        super(stream, null, 0, 0, false);
    }

    MessageQueueReader (T mq) {
        super (mq.getStream (), mq, mq.getHeadVirtualOffset (), BUFFER_SIZE, true);
        this.bufferFileOffset = this.getCurrentOffset();
    }

    public void                 setFilter (QuickMessageFilter filter, boolean isPolymorphic) {
        this.filter = filter;
        this.isPolymorphic = isPolymorphic;
    }

    public void                 enableRealTimeNotification() {
        this.realTimeNotifications = isRealTime = true;
    }

    protected boolean           accept () {
        if (filter == null)
            return (true);

        if (filter.acceptAllEntities ())
            return (true);

        int                     pos = mdi.getPosition ();

        if (isPolymorphic)
            mdi.skipBytes (1);

        mdi.readStringBuilder (symbol);

        boolean accepted = filter.acceptEntity (symbol);

        if (accepted)
            mdi.seek (pos);

        return (accepted);
    }

    /**
     * Returns true if there is new data and false if end of cursor reached.
     * If data is not available (yet) then:
     *      in async mode throws UnavailableResourceException;
     *      in sync mode blocks till there is new data.
     */
    private boolean          pageDataIn() {

        // friendly method; sets a bunch of member vars directly
        if (isAsynchronous ()) {
            NextResult next = mfile.read(this);

            if (next == NextResult.END_OF_CURSOR)
                return false;
            else if (next == NextResult.UNAVAILABLE)
                throw UnavailableResourceException.INSTANCE;
        }
        else {
            synchronized (this) {
                for (;;) {
                    NextResult next = mfile.read(this);

                    if (next == NextResult.UNAVAILABLE) {
                        try {
                            if (hasDataAvailable()) {
                                break;
                            } else {
                                // We don't have a complete message
                                if (available() == buffer.length) {
                                    // We can't receive message because it's size is greater than reader's beffer
                                    // => trigger buffer increase
                                    invalidateBuffer();
                                    // and continue
                                } else {
                                    // We don't have more data in the queue => wait for data.
                                    // Will be waken via RawReaderBase#submitNotifier
                                    wait();
                                    // and continue
                                }
                            }
                        } catch (InterruptedException ix) {
                            throw new UncheckedInterruptedException (ix);
                        }
                    } else if (next == NextResult.END_OF_CURSOR) {
                        return false;
                    } else {
                        break;
                    }
                }
            }
        }
        
        return true;
    }
    
    public int                  getBufferOffset() {
        return 0;
    }

    public int                  getBufferSize() {
        return buffer.length;
    }

    private boolean             hasDataAvailable() {
        long available = available();

        if (available == 0 || available < MessageSizeCodec.MAX_SIZE) // TODO: Is this correct comparison?
            return false;

        bodyLength = MessageSizeCodec.read(buffer, bufferPosition);
        return available >= MessageSizeCodec.fieldSize (bodyLength) + bodyLength;
    }

    protected abstract void     invalidateBuffer();

    public boolean              read () {
        for (;;) {
             // assuming that buffer not contains only full messages
            while (!hasDataAvailable()) {

                // assuming that buffer may be extended
                invalidateBuffer();

                if (!pageDataIn())
                    return false;
            }

            bodyLength = MessageSizeCodec.read(buffer, bufferPosition);

            bufferPosition += MessageSizeCodec.fieldSize (bodyLength);
            mdi.setBytes(buffer, bufferPosition, bodyLength);
            bufferPosition += bodyLength;

            TimeCodec.readTime (mdi, time);

            if (accept()) {
                if (DebugFlags.DEBUG_MSG_READ)
                    DebugFlags.read (
                        "TB DEBUG: read (): OK: file=" + describe() +
                        "; readAtOffset=" + (bufferFileOffset + bufferPosition) + 
                        "; num=" + bodyLength
                    );
                break;
            }

            if (DebugFlags.DEBUG_MSG_DISCARD)
                DebugFlags.discard (
                    "TB DEBUG: read (): REJECTED: file=" + describe() +
                    "; readAtOffset=" + (bufferFileOffset + bufferPosition) +
                    "; num=" + bodyLength
                );
        }

        return true;
    }

    protected String            describe() {
        return getClass().getSimpleName() + "@" + hashCode() + ": [" + mfile.describe() + "]";
    }

    @Override
    public String               toString() {
        return describe();
    }

    @Override
    public long                 available() {
        return currentFileOffset.get() - (bufferFileOffset + bufferPosition);
    }

    @Override
    public boolean              isTransient() {
        return true;
    }
}
