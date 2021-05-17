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

import com.epam.deltix.qsrv.hf.pub.TimeSource;

/**
 * @author Andy
 *         Date: Aug 12, 2010 9:59:10 AM
 */
public final class GlobalClock {

    private volatile TimeSource timeSource = RealTimeSource.INSTANCE;

    public static final GlobalClock INSTANCE = new GlobalClock();

    private GlobalClock () {}

    public long currentTimeMillis() {
        return timeSource.currentTimeMillis();
    }

    public void setTimeSource (TimeSource timeSource) {
        assert timeSource != null;
        this.timeSource = timeSource;
    }

    public TimeSource getTimeSource() {
        assert timeSource != null;
        return timeSource;
    }
}
