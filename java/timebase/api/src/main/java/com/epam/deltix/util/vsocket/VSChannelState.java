package com.epam.deltix.util.vsocket;

/**
 * Date: Mar 30, 2010
 */
public enum VSChannelState {
    /**
     *  Just created no handshake yet. Writing to output stream is possible,
     *  but might block waiting for handshake (with remote capacity report).
     *  Reading from input stream will obviously block waiting for data to be
     *  sent over.
     */
    NotConnected,

    /**
     *  Normal connected state.
     */
    Connected, 

    /**
     *  Remote endpoint has been closed. Writing to output stream will result in
     *  a {@link ChannelClosedException} being thrown. Reading from input stream
     *  will return all data that was sent prior to remote endpoint being closed,
     *  then EOF.
     */
    RemoteClosed,

    /**
     *  Local endpoint has been closed. Either reading or writing will
     *  immediately throw a {@link ChannelClosedException}.
     */
    Closed,

    /**
     *  Local close has been confirmed by remote side; therefore its id can
     *  now be re-used. To the caller, this state's behavior is identical to
     *  {@link #Closed}.
     */
    Removed
}
