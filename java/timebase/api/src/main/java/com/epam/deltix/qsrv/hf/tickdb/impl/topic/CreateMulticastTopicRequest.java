package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class CreateMulticastTopicRequest extends CreateTopicRequest {
    private final String endpointHost;
    private final Integer endpointPort;

    private final String networkInterface;
    private final Integer ttl;


    public CreateMulticastTopicRequest(String topicKey, List<RecordClassDescriptor> types, @Nullable Collection<? extends IdentityKey> initialEntitySet,
                                       @Nullable String targetStream,
                                       @Nullable String endpointHost, @Nullable Integer endpointPort,
                                       @Nullable String networkInterface, @Nullable Integer ttl) {
        super(topicKey, types, initialEntitySet, targetStream);
        this.endpointHost = endpointHost;
        this.endpointPort = endpointPort;
        this.networkInterface = networkInterface;
        this.ttl = ttl;
    }

    @Nullable
    public String getEndpointHost() {
        return endpointHost;
    }

    @Nullable
    public Integer getEndpointPort() {
        return endpointPort;
    }

    @Nullable
    public String getNetworkInterface() {
        return networkInterface;
    }

    @Nullable
    public Integer getTtl() {
        return ttl;
    }
}
