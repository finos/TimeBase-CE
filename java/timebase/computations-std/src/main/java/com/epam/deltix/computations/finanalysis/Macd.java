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

package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.MacdProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.ObjectStatefulFunctionBase;
import com.epam.deltix.computations.messages.MACDMessage;

@Function("MACD")
public class Macd extends ObjectStatefulFunctionBase<MACDMessage> {

    private MacdProcessor macd;

    public Macd() {
        super(MACDMessage::new);
    }

    @Init
    public void init(@Arg(defaultValue = "12") int fastPeriod,
                     @Arg(defaultValue = "26") int slowPeriod,
                     @Arg(defaultValue = "9")  int signalPeriod) {
        macd = new MacdProcessor(fastPeriod, slowPeriod, signalPeriod);
    }

    @Compute
    public void set(@BuiltInTimestampMs long timestamp, double v) {
        if (value == null) {
            value = buffer;
        }
        macd.add(v, timestamp);
        value.setHistogram(macd.histogram);
        value.setSignal(macd.signal);
        value.setValue(macd.value);
    }

    @Result
    @Type("OBJECT(com.epam.deltix.computations.messages.MACDMessage)")
    @Override
    public MACDMessage get() {
        return super.get();
    }
}

