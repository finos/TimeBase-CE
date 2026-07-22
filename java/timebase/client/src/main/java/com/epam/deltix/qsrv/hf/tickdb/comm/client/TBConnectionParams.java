package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * {@link com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient} connection parameters,
 * as if in {@link TickDBFactory#connect(String, int, boolean, String, String, Map)}.
 */
@ApiStatus.Internal
public class TBConnectionParams {
    private final String host;
    private final int port;
    private final boolean enableSSL;
    private final String user;
    private final String pass;
    private final Map<String, String> params;

    public TBConnectionParams(
            String host, int port, boolean enableSSL,
            @Nullable String user, @Nullable String pass,
            @Nullable Map<String, String> params
    ) {
        this.host = Objects.requireNonNull(host, "url cannot be null");
        this.port = port;
        this.enableSSL = enableSSL;
        this.user = user;
        this.pass = pass;
        this.params = params;
    }

    public boolean isEnableSSL() {
        return enableSSL;
    }

    public String getHost() {
        return host;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getPass() {
        return pass;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }
}
