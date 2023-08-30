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
package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;

/**
 * Message source that has indication of switching in real-time mode from historical.
 * 
 * Message source will emit RealTimeStartMessage when when realTimeAvailable() is true and
 * no ACTUAL data available at this moment:
 * for DURABLE streams - data files does not contain unread data,
 * for TRANSIENT streams - message buffer is exhausted.
 *
 * After invoking "reset()"  RealTimeMessageSource will produce at most one RealTimeStartMessage.
 */

public interface RealTimeMessageSource<T> extends MessageSource<T> {

    /**
     * @return true if this source already switched from historical to real-time data portion
     */
    boolean     isRealTime();

    /**
     *  @return true if source can be switched in real-time.
     *
     *  When realtime mode is available client can use method {@link #isRealTime()} ()}
     *  to detect switch from historical to real-time portion of data.
     *  Also in this mode client will receive special message {@link RealTimeStartMessage} .
     */
    boolean     realTimeAvailable();
}