package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.util.security.TimebaseAccessController;
import com.epam.deltix.util.security.SecurityController;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSConnectionListener;
import com.epam.deltix.util.concurrent.QuickExecutor;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: Mar 10, 2010
 */
public class VSConnectionHandler implements VSConnectionListener {
    private final DXTickDB          tickdb;
    private final Map<String, DXTickDB> userToDbCache = new HashMap<>();

    private final ServerParameters  params;
    private final SecurityContext   context;
    private final DXServerAeronContext aeronContext;
    private final AeronThreadTracker aeronThreadTracker;
    private final DirectTopicRegistry topicRegistry;

    public VSConnectionHandler(DXTickDB tickdb, ServerParameters params, @Nonnull DXServerAeronContext aeronContext, @Nonnull AeronThreadTracker aeronThreadTracker, @Nonnull DirectTopicRegistry topicRegistry) {
        this(tickdb, params, null, null, aeronContext, aeronThreadTracker, topicRegistry);
    }

    public VSConnectionHandler(DXTickDB tickdb, ServerParameters params, SecurityController controller, TimebaseAccessController ac, @Nonnull DXServerAeronContext aeronContext, @Nonnull AeronThreadTracker aeronThreadTracker, @Nonnull DirectTopicRegistry topicRegistry) {
        this.aeronContext = aeronContext;
        this.aeronThreadTracker = aeronThreadTracker;
        this.topicRegistry = topicRegistry;
        if (tickdb == null)
            throw new IllegalArgumentException ("db == null");

        context = controller != null ? new SecurityContext(controller, ac) : null;

        this.params = params;
        this.tickdb = tickdb;
    }

    @Override
    public void                 connectionAccepted (QuickExecutor executor, VSChannel serverChannel) {
        new RequestHandler (serverChannel, tickdb, userToDbCache, params, executor, context, aeronContext, aeronThreadTracker, topicRegistry).submit ();
    }
}
