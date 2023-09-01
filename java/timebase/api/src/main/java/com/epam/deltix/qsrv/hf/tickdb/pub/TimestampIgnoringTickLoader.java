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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *  Forces server-side setting of the timestamp of all messages
 *  passing through this loader, by resetting the timestamp to
 *  {@link TimeConstants#TIMESTAMP_UNKNOWN}
 */
public class TimestampIgnoringTickLoader extends FilterTickLoader<InstrumentMessage> {

    public TimestampIgnoringTickLoader (TickLoader<InstrumentMessage> delegate) {
        super (delegate);
    }

    @Override
    public void         send (InstrumentMessage msg) {
        msg.setTimeStampMs(TimeConstants.TIMESTAMP_UNKNOWN);
        super.send (msg);
    }
}