package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public abstract class BaseTopicRequest {
    private final String topicKey;

    public BaseTopicRequest(String topicKey) {
        this.topicKey = topicKey;
    }

    public String getTopicKey() {
        return topicKey;
    }
}
