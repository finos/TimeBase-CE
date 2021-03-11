package com.epam.deltix.util.vsocket;

import java.io.IOException;

/**
 *  Thrown from read or write methods when channel is explicitly
 *  closed by the other side (by calling close ()).
 */
public class ChannelClosedException extends IOException {
    public ChannelClosedException () {
    }
}
