package com.epam.deltix.qsrv.hf.tickdb.impl;


import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.ramdisk.FD;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.lang.Util;

import java.io.EOFException;
import java.io.IOException;

/**
 *
 */
public class RawReader<T extends FD> extends RawReaderBase <T> {

    private static final int                BUFFER_SIZE = 8192;

    public RawReader(TickStreamImpl stream, T mfile, long fileOffset, boolean live) {
        super(stream, mfile, fileOffset, BUFFER_SIZE, live);
    }

    /**
     *  The file offset of the buffer's first byte.
     */
    private long                            bufferFileOffset;
    private int                             bufferValidLength = 0;

    /**
     *  Make sure the buffer contains the segment starting at currentFileOffset,
     *  length bytes long, and return the corresponding start offset in
     *  the buffer. If currentFileOffset is at end of file, return -1.
     *  If currentFileOffset is not at the end of file, but there is not enough
     *  data in the file, throw an exception.
     */
    private int                 pageDataIn (int length, boolean eofOk)
        throws IOException
    {
        long        posInBuffer = currentFileOffset.get() - bufferFileOffset;

        if (posInBuffer >= 0 && posInBuffer + length <= bufferValidLength)
            return ((int) posInBuffer);

        synchronized (mfile) {

            int         lengthToRead;

            if (length < BUFFER_SIZE)
                lengthToRead = BUFFER_SIZE;
            else
                lengthToRead = length;

            if (buffer.length < lengthToRead)
                buffer = new byte [Util.doubleUntilAtLeast (buffer.length, lengthToRead)];

            bufferValidLength =
                mfile.read(currentFileOffset.get(), buffer, 0, lengthToRead);

            if (live && bufferValidLength <= 0)
                throw UnavailableResourceException.INSTANCE;

            //  Only after we have successfully returned from readIfAvailable
            //  can we set bufferFileOffset.
            bufferFileOffset = currentFileOffset.get();
        }

        if (eofOk) {
            if (bufferValidLength <= 0)
                return (-1);
        }
        else if (bufferValidLength < length)
            throw new EOFException();

        return (0);
    }

    /**
     * Reads a single message from the data file.
     *
     * @return  <code>false</code> if end of file reached.
     * @throws IOException
     */
    @Override
    public boolean              read() throws IOException {
        int         pos = pageDataIn (MessageSizeCodec.MAX_SIZE, true);

        if (pos < 0)
            return (false);

        //
        //  Point of no return. Read the message header. Note that even though
        //  the header may be smaller than MessageSizeCodec.MAX_SIZE,
        //  the entire message is always larger, so it is legal to expect that
        //  we can page in MessageSizeCodec.MAX_SIZE bytes from block start.
        //
        mdi.setBytes (buffer, pos, MessageSizeCodec.MAX_SIZE);
        bodyLength = MessageSizeCodec.read (mdi);

        final int   headSize = mdi.getPosition ();
        currentFileOffset.addAndGet(headSize);
        pos += headSize;

        if (pos + bodyLength > bufferValidLength) { // does this double-check help?
            pos = pageDataIn (bodyLength, false);
        }

        mdi.setBytes (buffer, pos, bodyLength);
        currentFileOffset.addAndGet(bodyLength);
//        //
//        //  mdi is now set up with message body.
//        //
//        TimeCodec.readTime(mdi, time);

        return (true);
    }


    @Override
    public long                 available() {
        return mfile.getLogicalLength() - currentFileOffset.get();
    }

    @Override
    public boolean              isTransient() {
        return false;
    }

    public void                 close () {
    }
}
