package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util;

import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.EchoMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.MessageWithNanoTime;

/**
 * @author Alexei Osipov
 */
public enum ExperimentFormat {
    ONE_TOPIC(false, MessageWithNanoTime.class),
    TWO_TOPIC_TWO_MESSAGES(true, EchoMessage.class),
    TWO_TOPIC_ONE_MESSAGE(true, MessageWithNanoTime.class);

    private final boolean echoTopic;
    private final Class echoMessageClass;

    ExperimentFormat(boolean echoTopic, Class echoMessageClass) {
        this.echoTopic = echoTopic;
        this.echoMessageClass = echoMessageClass;
    }

    public boolean useMainChannel() {
        return !echoTopic;
    }

    public boolean useEchoChannel() {
        return echoTopic;
    }

    public Class getEchoMessageClass() {
        return echoMessageClass;
    }
}
