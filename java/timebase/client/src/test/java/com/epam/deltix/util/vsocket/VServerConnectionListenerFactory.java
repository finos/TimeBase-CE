package com.epam.deltix.util.vsocket;

import java.io.DataOutputStream;
import com.epam.deltix.util.concurrent.QuickExecutor;

class VServerConnectionListenerFactory {
    static VSConnectionListener createLatencyListener() {
        return (executor, serverChannel) -> new VSockets_LatencyTest.LatencyServerThread(executor, serverChannel).submit();
    }

    static VSConnectionListener createEmptyListener() {
        return (executor, serverChannel) -> {
            //new ServerThread (executor, serverChannel).submit ();
        };
    }

    static VSConnectionListener createThroughputListener() {
        return (executor, serverChannel) -> new VSockets_ThroughputTest.ServerThread(executor, serverChannel).submit();
    }
}
