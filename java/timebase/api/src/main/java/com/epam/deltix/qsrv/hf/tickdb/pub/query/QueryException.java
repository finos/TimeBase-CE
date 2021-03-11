package com.epam.deltix.qsrv.hf.tickdb.pub.query;

/**
 *  An application exception (user-caused) during the execution of a
 *  query.
 */
public class QueryException extends RuntimeException {
    public QueryException (String message, Throwable cause) {
        super (message, cause);
    }

    public QueryException (String message) {
        super (message);
    }

    public QueryException () {
    }
}
