package com.epam.deltix.util.vsocket;

import java.io.IOException;

/**
 * Signals that Timebase server was restarted during reconnecting.
 */
public class ServerRestartedException extends IOException {

     public ServerRestartedException (String serverId, long time) {
        super ("Server " + serverId + " was restarted at " + time);                
    }
}
