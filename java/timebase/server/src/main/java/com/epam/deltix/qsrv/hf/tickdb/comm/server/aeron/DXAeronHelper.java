package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron;

import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.aeron.DXAeron;
import com.epam.deltix.util.vsocket.TransportProperties;

/**
 * @author Alexei Osipov
 */
public class DXAeronHelper {
    public static void start(boolean startMediaDriver) {
        DXAeron.start(Home.getPath("temp/dxipc"), startMediaDriver);
    }
}
