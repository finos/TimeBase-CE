package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.MessageWithNanoTime;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.NanoTimeSource;
import com.epam.deltix.util.time.TimeKeeper;
import org.HdrHistogram.Histogram;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * @author Alexei Osipov
 */
public abstract class WriteBase {
    abstract MessageChannel<InstrumentMessage> createLoader(RemoteTickDB client);

    public long runLoader(BooleanSupplier stopSignal, int loaderWarmUpTime, int loaderTimeToRun, int loaderMessageRatePerMs, RemoteTickDB client, String generatorMode, int experimentId) {

        // Create loader
        MessageChannel<InstrumentMessage> loader = createLoader(client);

        MessageWithNanoTime msg = new MessageWithNanoTime();
        msg.setExperimentId(experimentId);

        System.out.println("generatorMode=" + generatorMode);
        System.out.println("loaderWarmUpTime=" + loaderWarmUpTime);
        System.out.println("loaderTimeToRun=" + loaderTimeToRun);
        System.out.println("loaderMessageRatePerMs=" + loaderMessageRatePerMs);
        System.out.println("Sending messages with size=" + msg.getMessageSizeEstimate());


        Histogram seqMsgHistogram = new Histogram(3);
        Histogram seqYieldHistogram = new Histogram(3);

        System.out.println("Sending messages...");

        long msgCount;
        switch (generatorMode) {
            case "tk-ms-yield":
                msgCount = generateMessages1(stopSignal, loaderWarmUpTime, loaderTimeToRun, loaderMessageRatePerMs, loader, msg, seqMsgHistogram, seqYieldHistogram, true, experimentId);
                break;
            case "tk-ms-spin":
                msgCount = generateMessages1(stopSignal, loaderWarmUpTime, loaderTimeToRun, loaderMessageRatePerMs, loader, msg, seqMsgHistogram, seqYieldHistogram, false, experimentId);
                break;
            case "nanos-yield":
                msgCount = generateMessages2(stopSignal, loaderWarmUpTime, loaderTimeToRun, loaderMessageRatePerMs, loader, msg, seqMsgHistogram, seqYieldHistogram, true, experimentId);
                break;
            case "nanos-spin":
                msgCount = generateMessages2(stopSignal, loaderWarmUpTime, loaderTimeToRun, loaderMessageRatePerMs, loader, msg, seqMsgHistogram, seqYieldHistogram, false, experimentId);
                break;
            default:
                throw new RuntimeException();
        }
        loader.close();

        System.out.println("=========");
        System.out.println("Seq message sent counts:");
        seqMsgHistogram.outputPercentileDistribution(System.out, 1.0);
        System.out.println("=========");
        System.out.println("Seq yields:");
        seqYieldHistogram.outputPercentileDistribution(System.out, 1.0);

        return msgCount;
    }

    private long generateMessages1(BooleanSupplier stopSignal, int loaderWarmUpTime, int loaderTimeToRun, int loaderMessageRatePerMs,
                                   MessageChannel<InstrumentMessage> loader, MessageWithNanoTime msg,
                                   Histogram seqMsgHistogram, Histogram seqIdleHistogram, boolean yield, int experimentId) {
        long startTime = TimeKeeper.currentTime;
        long measureStartTime = startTime + TimeUnit.SECONDS.toMillis(loaderWarmUpTime);
        long stopTime = measureStartTime + TimeUnit.SECONDS.toMillis(loaderTimeToRun);
        long msgCount = 0;
        long currentTime = startTime;
        Random rng = new Random(0);
        long seqMessages = 0;
        long seqIdle = 0;
        while (!stopSignal.getAsBoolean() && currentTime < stopTime) {
            currentTime = TimeKeeper.currentTime;

            if ((currentTime - startTime) * loaderMessageRatePerMs < msgCount) {
                if (seqMessages > 0) {
                    seqMsgHistogram.recordValue(seqMessages);
                    seqMessages = 0;
                }
                seqIdle++;
                if (yield) {
                    Thread.yield();
                }
            } else {
                if (seqIdle > 0) {
                    seqIdleHistogram.recordValue(seqIdle);
                    seqIdle = 0;
                }
                msgCount++;
                seqMessages++;
                boolean measure = currentTime >= measureStartTime;
                sendMessage(msgCount, loader, msg, currentTime, rng, measure, experimentId);
            }
        }
        return msgCount;
    }

    private long generateMessages2(BooleanSupplier stopSignal, int loaderWarmUpTime, int loaderTimeToRun, int loaderMessageRatePerMs,
                                   MessageChannel<InstrumentMessage> loader, MessageWithNanoTime msg,
                                   Histogram seqMsgHistogram, Histogram seqIdleHistogram, boolean yield, int experimentId) {
        long startTime = NanoTimeSource.getNanos();
        long measureStartTime = startTime + TimeUnit.SECONDS.toNanos(loaderWarmUpTime);
        long stopTime = measureStartTime + TimeUnit.SECONDS.toNanos(loaderTimeToRun);
        long msgCount = 0;
        long currentNanoTime = startTime;
        Random rng = new Random(0);
        long seqMessages = 0;
        long seqIdle = 0;
        while (!stopSignal.getAsBoolean() && currentNanoTime < stopTime) {
            currentNanoTime = NanoTimeSource.getNanos();

            if ((currentNanoTime - startTime) * loaderMessageRatePerMs < msgCount * 1_000_000) {
                if (seqMessages > 0) {
                    seqMsgHistogram.recordValue(seqMessages);
                    seqMessages = 0;
                }
                seqIdle++;
                if (yield) {
                    Thread.yield();
                }
            } else {
                if (seqIdle > 0) {
                    seqIdleHistogram.recordValue(seqIdle);
                    seqIdle = 0;
                }
                msgCount++;
                seqMessages++;
                boolean measure = currentNanoTime >= measureStartTime;
                sendMessage(msgCount, loader, msg, TimeKeeper.currentTime, rng, measure, experimentId);
            }
        }
        return msgCount;
    }

    private static void sendMessage(long messageId, MessageChannel<InstrumentMessage> loader, MessageWithNanoTime msg, long currentTime, Random rng, boolean measure, int experimentId) {
        if (measure) {
            msg.setExperimentId(experimentId);
        } else {
            msg.setExperimentId(0); // Exclude from result
        }
        msg.setMessageId(messageId);
        msg.setTimeStampMs(currentTime);


        // We don't add nano time to each message. We add it to a fraction of messages.
        // We don't use "timestamp each Nth message" strategy because it will make entire measurement more biased.
        // So we pick messages at random with specified probability
        boolean includeNanos = chosenForSample(rng, DemoConf.FRACTION_OF_MARKED);
        if (includeNanos) {
            // Note: this is relatively costly operation. Calling nanoTime may be the main pipeline bottleneck.
            msg.setPublisherNanoTime(NanoTimeSource.getNanos());
        } else {
            msg.setPublisherNanoTime(0);
        }

        // Send message to topic
        loader.send(msg);
    }

    private static boolean chosenForSample(Random rng, int fractionOfMarked) {
        return fractionOfMarked == 1 || rng.nextInt(fractionOfMarked) == 0;
    }
}
