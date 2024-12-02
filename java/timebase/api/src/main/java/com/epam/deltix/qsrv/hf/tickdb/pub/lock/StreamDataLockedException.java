/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.pub.lock;

import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingError;
import com.epam.deltix.util.time.GMT;

/**
 *
 */
public class StreamDataLockedException extends LoadingError {

    public static String buildMessage(String stream, DBLock lock, long startTime, long endTime) {
        return String.format("Stream %s is locked by lock %s (Trying to update interval: [%s -> %s])",
            stream, lock, GMT.formatDateTimeMillis(startTime), GMT.formatDateTimeMillis(endTime)
        );
    }

    public static String buildMessage(String stream, DBLock lock, long time) {
        return String.format("Stream %s is locked by lock %s (Trying to update timestamp: %s)",
            stream, lock, GMT.formatDateTimeMillis(time)
        );
    }

    public StreamDataLockedException() {
    }

    public StreamDataLockedException(String message) {
        super(message);
    }

    public StreamDataLockedException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamDataLockedException(Throwable cause) {
        super(cause);
    }

}