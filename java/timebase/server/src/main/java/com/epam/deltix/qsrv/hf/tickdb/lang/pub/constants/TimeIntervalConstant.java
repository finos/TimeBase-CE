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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TimeIntervalUtil;
import com.epam.deltix.util.annotations.TimestampMs;
import com.epam.deltix.util.annotations.TimestampNs;
import com.epam.deltix.util.lang.Util;

public class TimeIntervalConstant extends Constant {

    private final String mnemonic;
    private final long timestampMs;
    private final long nanoTime;

    public TimeIntervalConstant(long location, String value) {
        super(location);
        this.mnemonic = value;
        this.timestampMs = TimeIntervalUtil.parseMs(value);
        this.nanoTime = TimeIntervalUtil.parseNs(value);
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        s.append(mnemonic);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TimeIntervalConstant))
            return false;
        return nanoTime == ((TimeIntervalConstant) obj).nanoTime;
    }

    @Override
    public int hashCode() {
        return Util.hashCode(nanoTime);
    }

    public String getMnemonic() {
        return mnemonic;
    }

    @TimestampMs
    public long getTimeStampMs() {
        return timestampMs;
    }

    @TimestampNs
    public long getNanoTime() {
        return nanoTime;
    }
}