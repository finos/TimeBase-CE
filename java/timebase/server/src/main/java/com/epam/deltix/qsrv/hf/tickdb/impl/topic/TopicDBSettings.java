package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.util.concurrent.QuickExecutor;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicDBSettings {
    private final DXServerAeronContext aeronContext;
    private final DirectTopicRegistry topicRegistry;
    private final QuickExecutor executor;

    public TopicDBSettings(DXServerAeronContext aeronContext, DirectTopicRegistry topicRegistry, QuickExecutor executor) {
        this.topicRegistry = topicRegistry;
        this.aeronContext = aeronContext;
        this.executor = executor;
    }

    public DXServerAeronContext getAeronContext() {
        return aeronContext;
    }

    public DirectTopicRegistry getTopicRegistry() {
        return topicRegistry;
    }

    public QuickExecutor getExecutor() {
        return executor;
    }
}
