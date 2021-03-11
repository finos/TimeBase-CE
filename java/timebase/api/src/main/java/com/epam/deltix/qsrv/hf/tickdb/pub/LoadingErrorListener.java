package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 * Listener interface for errors related to loading/sending data into a
 *  {@link TickLoader}.
 */
public interface LoadingErrorListener {
    /**
     *  Error in loaded message.
     *
     *  @param e    Error details.
     */
    public void onError (LoadingError e);
}
