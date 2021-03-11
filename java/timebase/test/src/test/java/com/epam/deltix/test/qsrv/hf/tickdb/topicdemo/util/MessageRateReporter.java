package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util;

import com.epam.deltix.util.time.TimeKeeper;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
public class MessageRateReporter {
    private static final int checkTimeEachMessages = 1_000;
    private static final long timeIntervalMs = TimeUnit.SECONDS.toMillis(10);
    private final int consumerNumber;

    private int msgCount = 0;
    private long startTime;
    private long prevTime;

    public MessageRateReporter(int consumerNumber) {
        this.consumerNumber = consumerNumber;
        this.startTime = getCurrentTime();
        this.prevTime = startTime;
    }

    public void addMessage() {
        msgCount++;
        if (msgCount % checkTimeEachMessages == 0) {
            long currentTime = getCurrentTime();
            long timeDelta = currentTime - prevTime;

            if (timeDelta > timeIntervalMs) {
                long secondsFromStart = (currentTime - startTime) / 1000;
                //System.out.println("#" + consumerNumber + ": Message rate: " + ((float) Math.round(msgCount * 1000 / timeDelta))/1000 + " k msg/s");
                System.out.printf("%6d: #%s: Message rate: %.3f k msg/s\n", secondsFromStart, consumerNumber, ((float) msgCount) / timeDelta);
                prevTime = currentTime;
                msgCount = 0;
            }
        }
    }

    private long getCurrentTime() { // Ms
        return TimeKeeper.currentTime;
    }
}
