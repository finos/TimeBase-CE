package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alexei Osipov
 */
public class CreateTopicResult {
    private final AtomicBoolean topicDeletedSignal;
    private final CountDownLatch copyToThreadStopLatch;

    CreateTopicResult(AtomicBoolean topicDeletedSignal, CountDownLatch copyToThreadStopLatch) {
        this.topicDeletedSignal = topicDeletedSignal;
        this.copyToThreadStopLatch = copyToThreadStopLatch;
    }

    public AtomicBoolean getTopicDeletedSignal() {
        return topicDeletedSignal;
    }

    public CountDownLatch getCopyToThreadStopLatch() {
        return copyToThreadStopLatch;
    }
}
