package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

/**
 * @author Alexei Osipov
 */
public class AddTopicSubscriberRequest {
    private final String topicKey;

    public AddTopicSubscriberRequest(String topicKey) {
        this.topicKey = topicKey;
    }

    public String getTopicKey() {
        return topicKey;
    }
}
