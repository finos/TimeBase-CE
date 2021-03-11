package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.pub.ChannelCompression;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.UserPrincipal;
import com.epam.deltix.util.vsocket.VSChannel;

import java.io.IOException;

/**
 *
 */
public class SecuredDbClient extends TickDBClient {

    public SecuredDbClient(String host, int port, boolean ssl, String user, String pass) {
        super(host, port, ssl, user, pass);
    }

    public SecuredDbClient(TickDBClient client) {
        super(client.getHost(), client.getPort(), client.enableSSL, new UserPrincipal(client.getUser()));
    }

    public VSChannel               connect(ChannelType type, boolean autoCommit, boolean noDelay, ChannelCompression c, int channelBufferSize)
            throws IOException
    {
        VSChannel channel = createChannel(type, autoCommit, noDelay, c, 0);
        TDBProtocol.writeCredentials(channel, getUser(), UserContext.get());
        return channel;
    }
}
