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
package com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer.VirtualTimePeriodicTaskExecutor;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

/**
 * @author Alexei Osipov
 */
public class VirtualTimePeriodicTaskExecutorTest {


    @Test
    public void testTaskExecutionSpeed() throws Exception {
        int speed1 = 5;
        AtomicInteger counter = new AtomicInteger(0);
        VirtualTimePeriodicTaskExecutor virtualClock = new VirtualTimePeriodicTaskExecutor(speed1, 1000, virtualCLockTime -> counter.incrementAndGet(), null, null);
        virtualClock.startFromTimestamp(0);

        Thread.sleep(2000);

        virtualClock.stop();
        int counterValue = counter.get();
        System.out.println("counterValue: " + counterValue);
        assertTrue("Expected 10, got " + counterValue, 9 <= counterValue && counterValue <= 11);
    }

}