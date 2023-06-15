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
package com.epam.deltix.qsrv.util.time;

import com.epam.deltix.qsrv.util.time.PreciseTimeSource;
import org.junit.Test;

import java.util.Date;

/**
 * @author Andy
 *         Date: Nov 19, 2010 1:28:11 PM
 */
public class Test_PreciseTimeSource {

    @Test
    public void test1 () throws InterruptedException {
        PreciseTimeSource ts = new PreciseTimeSource ();


        final int N = 10000000;
        long delta [] = new long [N];
        for (int i=0; i < N; i++) {
            long now1 = System.currentTimeMillis();
            long now2 = ts.currentTimeMillis();
            delta[i] = now1 - now2;
        }
        for (int i=0; i < N; i++) {
            if (delta[i] != 0)
                System.out.println("#A#:" + i + " " + delta[i]);
        }

        for (int i=0; i < N; i++) {
            long now1 = System.currentTimeMillis();
            long now2 = ts.currentTimeMillis();
            delta[i] = now1 - now2;
            Thread.sleep(0);
        }

        for (int i=0; i < N; i++) {
            if (delta[i] != 0)
                System.out.println("#B#:" + i + " " + delta[i]);
        }


        System.out.println("Done " + ts);
        while(true) {
            long now1 = System.currentTimeMillis();
            long now2 = ts.currentTimeMillis();
            if (Math.abs(now1 - now2) > 2) {
                System.out.println("#delta at #:" + new Date(now1) + " " + (now1 - now2));
                Thread.sleep(5000);
            }

        }
    }
}