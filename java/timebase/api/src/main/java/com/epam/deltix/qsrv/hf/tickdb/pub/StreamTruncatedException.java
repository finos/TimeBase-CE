package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *  Unchecked exception thrown by a cursor when the current message
 *  has been deleted due to stream truncation.
 */
public class StreamTruncatedException extends CursorException {
    public final String             streamKey;
    public final String             fileId;
    public final long               offset;
    public final long               nanoTime;

    public StreamTruncatedException (String streamKey, String fileId, long offset, long nanoTime) {
        super ("File " + fileId + " in stream " + streamKey + " has been truncated.");
        this.streamKey = streamKey;
        this.fileId = fileId;
        this.offset = offset;
        this.nanoTime = nanoTime;
    }
}