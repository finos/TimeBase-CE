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

/**
 * @author Alexei Osipov
 */
public class NanoTimeSource {

    public static long getNanos() {
        switch (DemoConf.LATENCY_CLOCK_TYPE) {
            case SYSTEM_NANO_TIME:
                return System.nanoTime();
            case TIME_KEEPER_HIGH_PRECISION:
                return TimeKeeper.currentTimeNanos;
            default:
                throw new IllegalStateException();
        }
    }

}