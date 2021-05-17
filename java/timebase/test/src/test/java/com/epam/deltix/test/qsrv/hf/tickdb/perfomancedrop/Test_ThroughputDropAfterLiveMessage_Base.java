/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

/**
 * Base class for PD tests.
 * See {@link deltix.qsrv.hf.tickdb.perfomancedrop} for details.
 *
 * @author Alexei Osipov
 */
abstract class Test_ThroughputDropAfterLiveMessage_Base {

    final DecimalFormat df = new DecimalFormat("#,###");
    final PrintStream fileOut = new PrintStream(new FileOutputStream("out_log.txt"));
    PrintStream systemOut;

    protected Test_ThroughputDropAfterLiveMessage_Base() throws FileNotFoundException {
    }

    protected void print(String x) {
        systemOut.println(x);
        fileOut.println(x);
        fileOut.flush();
    }

    protected void printTimed(String x) {
        print(StringUtils.leftPad(getUptimePrefixString(), 9, ' ') + x);
    }

    protected static long getUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    protected static String getUptimeString() {
        return Long.toString(getUptime());
    }

    protected String getUptimePrefixString() {
        return df.format(getUptime()) + ": ";
    }




    @Before
    public void startup() throws Throwable {
        systemOut = System.out;
        System.setOut(fileOut);
    }

    @After
    public void shutdown() throws Throwable {
        System.setOut(systemOut);
        fileOut.flush();
        systemOut.flush();
    }

    protected Integer getMean(List<Integer> measurements) {
        Collections.sort(measurements);
        int size = measurements.size();
        int middleIndex = size / 2;
        if (size % 2 == 0) {
            // Even size => average two values in the middle
            return (measurements.get(middleIndex) + measurements.get(middleIndex + 1)) / 2;
        } else {
            // Odd size => just use middle
            return measurements.get(middleIndex);
        }
    }

    protected Integer getMin(List<Integer> measurements, int position) {
        Collections.sort(measurements);
        return measurements.get(position - 1);
    }

    protected Integer getMax(List<Integer> measurements, int position) {
        Collections.sort(measurements, Collections.reverseOrder());
        return measurements.get(position - 1);
    }

    protected float getLossPercent(float before, float after) {
        return ((float) Math.round(1000 - (after * 1000) / before)) / 10;
    }

    protected static TickCursor getTickCursor(DXTickStream stream, boolean useFixedStreamTypeCursor) {
        SelectionOptions options = new SelectionOptions(true, false);
        options.restrictStreamType = useFixedStreamTypeCursor;
        TickCursor cursor = stream.createCursor(options);
        cursor.reset(Long.MIN_VALUE);
        cursor.subscribeToAllEntities();
        return cursor;
    }
}
