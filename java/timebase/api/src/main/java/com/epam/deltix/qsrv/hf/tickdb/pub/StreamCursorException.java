package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *  Unchecked exception thrown by a cursor  e.g. cursor.next()
 */
public class StreamCursorException extends CursorException {

    public StreamCursorException(String message) {
        super (message);
    }

    public StreamCursorException(String message, Throwable cause) {
        super(message, cause);
    }
}