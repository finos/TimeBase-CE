package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import java.util.List;

/**
 * @author Alexei Osipov
 */
public class ListTopicsResponse {
    private final List<String> topics;

    public ListTopicsResponse(List<String> topics) {
        this.topics = topics;
    }

    public List<String> getTopics() {
        return topics;
    }
}
