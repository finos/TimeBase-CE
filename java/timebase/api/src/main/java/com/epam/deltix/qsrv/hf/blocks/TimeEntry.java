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
package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

public class TimeEntry implements TimeIdentity {
    public long                         timestamp;
    public String                       symbol;

    public  TimeEntry() {
    }

    public  TimeEntry(IdentityKey id, long timestamp) {
        this.symbol = id.getSymbol().toString();
        this.timestamp = timestamp;
    }

    @Override
    public TimeEntry            get(IdentityKey id) {
        return this;        
    }

    @Override
    public TimeIdentity         create(IdentityKey id) {
        return new TimeEntry(id, TimeStampedMessage.TIMESTAMP_UNKNOWN);
    }

    @Override
    public CharSequence         getSymbol() {
        return symbol;
    }

    @Override
    public long                 getTime() {
        return timestamp;
    }

    @Override
    public void                 setTime(long timestamp) {
        this.timestamp = timestamp;
    }
}