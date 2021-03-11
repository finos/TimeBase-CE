package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

/**
 * @author Alexei Osipov
 */
public class DeleteTopicRequest {
    private final String topicKey;

    public DeleteTopicRequest(String topicKey) {
        this.topicKey = topicKey;
    }

    public String getTopicKey() {
        return topicKey;
    }
}
