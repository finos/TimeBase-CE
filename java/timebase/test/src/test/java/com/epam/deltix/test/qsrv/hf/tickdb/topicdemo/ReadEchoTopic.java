package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo;

import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.ExperimentFormat;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.BooleanSupplier;

/**
 * @author Alexei Osipov
 */
public class ReadEchoTopic extends ReadEchoBase {
    public ReadEchoTopic(ExperimentFormat timestampGetter) {
        super(timestampGetter);
    }

    @Override
    protected void work(RemoteTickDB client, BooleanSupplier stopCondition, MessageProcessor processor) {
        String topicKey = experimentFormat.useMainChannel() ? DemoConf.DEMO_MAIN_TOPIC :  DemoConf.DEMO_ECHO_TOPIC;
        MessagePoller messagePoller = client.getTopicDB().createPollingConsumer(topicKey, null);

        try {
            IdleStrategy idleStrategy = DemoConf.getReaderIdleStrategy();
            while (!stopCondition.getAsBoolean()) {
                idleStrategy.idle(messagePoller.processMessages(100, processor));
            }
        } finally {
            messagePoller.close();
        }
    }
}
