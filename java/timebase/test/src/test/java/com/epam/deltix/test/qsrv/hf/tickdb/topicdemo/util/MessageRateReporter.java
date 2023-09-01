/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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