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
package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.InstrumentMessageSourceAdapter;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableCursor;
import com.epam.deltix.util.concurrent.NextResult;

/**
 * MessageSource that simulates end-of-stream at given timestamp
 */
public class LimitedInstrumentMessageSource extends InstrumentMessageSourceAdapter implements IntermittentlyAvailableCursor {
    private final long limit;
    private boolean keepReading = true;
    private final IntermittentlyAvailableCursor iac;

    public LimitedInstrumentMessageSource(InstrumentMessageSource delegate, long endTime) {
        super(delegate);

        this.limit = endTime;
        this.iac = (delegate instanceof IntermittentlyAvailableCursor) ? (IntermittentlyAvailableCursor)delegate : null;
    }

    @Override
    public InstrumentMessage getMessage() {
        assert keepReading;
        return super.getMessage();
    }

    @Override
    public boolean isAtEnd() {
        return ! keepReading;
    }

    @Override
    public boolean next() {
        if (keepReading) {
            keepReading = super.next();
            if (keepReading) {
                InstrumentMessage msg = super.getMessage();
                keepReading = (msg.getTimeStampMs() < limit);
            }
        }
        return keepReading;
    }

    @Override
    public NextResult nextIfAvailable() {

        if (keepReading) {
            NextResult result = iac.nextIfAvailable();

            keepReading = result == NextResult.OK;
            if (keepReading) {
                InstrumentMessage msg = super.getMessage();
                keepReading = (msg.getTimeStampMs() < limit);
            }

            return result;
        }

        return NextResult.END_OF_CURSOR;
    }
}
