package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.concurrent.CountDownLatch;

/**
 * @author Alexei Osipov
 */
public class ReadAndReplyTopic extends ReadAndReplyBase {

    @Override
    MessageChannel<InstrumentMessage> createReplyLoader(RemoteTickDB client) {
        return client.getTopicDB().createPublisher(DemoConf.DEMO_ECHO_TOPIC, null, new BusySpinIdleStrategy());
    }

    @Override
    void work(RemoteTickDB client, CountDownLatch stopSignal, MessageProcessor messageProcessor) {
        MessagePoller messagePoller = client.getTopicDB().createPollingConsumer(DemoConf.DEMO_MAIN_TOPIC, null);

        IdleStrategy idleStrategy = DemoConf.getReaderIdleStrategy();

        // Process messages from topic until stop signal triggered
        while (stopSignal.getCount() > 0) {
            idleStrategy.idle(messagePoller.processMessages(100, messageProcessor));
        }
        messagePoller.close();
    }
}
