package com.epam.deltix.util.collections.latency;

import com.epam.deltix.util.vsocket.TransportType;

/**
 *
 */
public interface TestSettings {
    static boolean USE_VSOCKET = true;
    static TransportType TRANSPORT_TYPE = TransportType.OFFHEAP_IPC;
    static boolean AERON_UDP = false;

    static boolean LAUNCH_SERVER_THREAD = true;

    static String AERON_UDP_URL = "udp://localhost:50123";

    static String CLIENT_FILE = "client.tmp";
    static String SERVER_FILE = "server.tmp";

    static int THROUGHPUT = Integer.getInteger("throughput", 10000);
    static int REPS = Integer.getInteger("reps", 100000);
    static int WARMUP = Integer.getInteger("warmup", 100000);

    static int PORT = Integer.getInteger("port", 7788);
}
