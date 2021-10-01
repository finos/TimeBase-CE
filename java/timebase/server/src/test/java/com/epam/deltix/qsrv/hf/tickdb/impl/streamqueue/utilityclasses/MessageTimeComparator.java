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
package com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.utilityclasses;

import com.epam.deltix.timebase.messages.TimeStampedMessage;

import java.util.Comparator;

/**
 * @author Alexei Osipov
 */
public final class MessageTimeComparator<T extends TimeStampedMessage> implements Comparator<T> {
    @Override
    public final int compare(T o1, T o2) {
        long time1 = o1.getNanoTime();
        long time2 = o2.getNanoTime();

        // Unfortunately there is no easy way to simplify this. See Long.compare()
        return Long.compare(time1, time2);
    }
}