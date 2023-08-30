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

import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer.ManualClock;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer.SimpleVirtualClock;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDBFast.class)
public class SimpleVirtualClockTest {

    @Test
    public void testVirtualClockSpeedChange() throws Exception {
        ManualClock systemClockStub = new ManualClock();
        int speed1 = 2;
        SimpleVirtualClock virtualClock = new SimpleVirtualClock(speed1, systemClockStub);

        long stubTime = TimeUnit.DAYS.toMillis(100);
        systemClockStub.setCurrentTimeMillis(stubTime);

        long baseVirtualTime = TimeUnit.DAYS.toMillis(500);

        virtualClock.resetTo(baseVirtualTime);

        // Assert initial time is right
        assertEquals(baseVirtualTime, virtualClock.currentTimeMillis());

        // Advance "real" time
        stubTime += TimeUnit.HOURS.toMillis(3);
        systemClockStub.setCurrentTimeMillis(stubTime);

        long expectedVirtualTime2 = baseVirtualTime + TimeUnit.HOURS.toMillis(3 * speed1);
        assertEquals(expectedVirtualTime2, virtualClock.currentTimeMillis());

        // Change speed
        int speed2 = 5;
        virtualClock.setSpeed(speed2);

        assertEquals(
                "Virtual time should not change after clock speed change",
                expectedVirtualTime2, virtualClock.currentTimeMillis()
        );

        // Advance "real" time
        stubTime += TimeUnit.MILLISECONDS.toMillis(7);
        systemClockStub.setCurrentTimeMillis(stubTime);

        long expectedVirtualTime3 = expectedVirtualTime2 + TimeUnit.MILLISECONDS.toMillis(7 * speed2);
        assertEquals(expectedVirtualTime3, virtualClock.currentTimeMillis());
    }

    @Test
    public void testPause() throws Exception {
        ManualClock systemClockStub = new ManualClock();
        int speed1 = 2;
        SimpleVirtualClock virtualClock = new SimpleVirtualClock(speed1, systemClockStub);

        long stubTime = TimeUnit.DAYS.toMillis(100);
        systemClockStub.setCurrentTimeMillis(stubTime);

        long baseVirtualTime = TimeUnit.DAYS.toMillis(500);

        virtualClock.resetTo(baseVirtualTime);
        virtualClock.pause();

        stubTime += TimeUnit.HOURS.toMillis(3);
        systemClockStub.setCurrentTimeMillis(stubTime);

        assertEquals(baseVirtualTime, virtualClock.currentTimeMillis());

        virtualClock.resume();

        assertEquals(baseVirtualTime, virtualClock.currentTimeMillis());

        stubTime += TimeUnit.MINUTES.toMillis(13);
        systemClockStub.setCurrentTimeMillis(stubTime);

        long expectedVirtualTime = baseVirtualTime + TimeUnit.MINUTES.toMillis(13 * speed1);
        assertEquals(expectedVirtualTime, virtualClock.currentTimeMillis());
    }

    @Test
    public void testSpeed0() throws Exception {
        ManualClock systemClockStub = new ManualClock();
        int speed1 = 2;
        SimpleVirtualClock virtualClock = new SimpleVirtualClock(speed1, systemClockStub);

        long stubTime = TimeUnit.DAYS.toMillis(100);
        systemClockStub.setCurrentTimeMillis(stubTime);

        long baseVirtualTime = TimeUnit.DAYS.toMillis(500);

        virtualClock.resetTo(baseVirtualTime);
        virtualClock.setSpeed(0);

        stubTime += TimeUnit.HOURS.toMillis(3);
        systemClockStub.setCurrentTimeMillis(stubTime);

        assertEquals(baseVirtualTime, virtualClock.currentTimeMillis());

        virtualClock.setSpeed(speed1);

        assertEquals(baseVirtualTime, virtualClock.currentTimeMillis());

        stubTime += TimeUnit.MINUTES.toMillis(13);
        systemClockStub.setCurrentTimeMillis(stubTime);

        long expectedVirtualTime = baseVirtualTime + TimeUnit.MINUTES.toMillis(13 * speed1);
        assertEquals(expectedVirtualTime, virtualClock.currentTimeMillis());
    }

}