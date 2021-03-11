package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.EchoMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.MessageWithNanoTime;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.ExperimentFormat;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.MessageRateReporter;

import java.util.concurrent.CountDownLatch;

/**
 * @author Alexei Osipov
 */
public abstract class ReadAndReplyBase {

    abstract MessageChannel<InstrumentMessage> createReplyLoader(RemoteTickDB client);

    abstract void work(RemoteTickDB client, CountDownLatch stopSignal, MessageProcessor messageProcessor);

    public void runReader(RemoteTickDB client, CountDownLatch stopSignal, byte consumerNumber, boolean printReadRate, ExperimentFormat experimentFormat) {
        MessageRateReporter rateReporter = new MessageRateReporter(consumerNumber);

        MessageChannel<InstrumentMessage> replyLoader = createReplyLoader(client);

        EchoMessage echoMessage = new EchoMessage();

        boolean copyOriginalMessages = experimentFormat.getEchoMessageClass().equals(MessageWithNanoTime.class);

        MessageProcessor messageProcessor = new MessageProcessor() {
            @Override
            public void process(InstrumentMessage message) {
                // Do what you need to do with message

                MessageWithNanoTime messageWithNanoTime = (MessageWithNanoTime) message;
                if (messageWithNanoTime.getPublisherNanoTime() > 0) {
                    if (copyOriginalMessages) {
                        replyLoader.send(message);
                    } else {
                        // Build and send reply
                        fillReplyMessage(messageWithNanoTime, echoMessage, consumerNumber);
                        replyLoader.send(echoMessage);
                    }
                }

                if (printReadRate) {
                    // Count message
                    rateReporter.addMessage();
                }
            }
        };

        work(client, stopSignal, messageProcessor);
    }

    private static void fillReplyMessage(MessageWithNanoTime message, EchoMessage echoMessage, byte consumerNumber) {
        echoMessage.setExperimentId(message.getExperimentId());
        echoMessage.setConsumerNumber(consumerNumber);
        echoMessage.setOriginalTimeStamp(message.getTimeStampMs());
        echoMessage.setOriginalNanoTime(message.getPublisherNanoTime());
        echoMessage.setOriginalMessageId(message.getMessageId());

        echoMessage.setSymbol(message.getSymbol());
        echoMessage.setTimeStampMs(0);
        //echoMessage.setTimeStampMs(TimeKeeper.currentTime);
    }
}
