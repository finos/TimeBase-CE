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
package com.epam.deltix.data.stream.pq.utilityclasses;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

import java.util.Comparator;

/**
 * @author Alexei Osipov
 */
public class MessageSourceComparator<T extends TimeStampedMessage> implements Comparator<MessageSource<T>> {
    @Override
    public int compare(MessageSource<T> o1, MessageSource<T> o2) {
        TimeStampedMessage m1 = o1.getMessage();
        TimeStampedMessage m2 = o2.getMessage();
        long time1 = m1.getNanoTime();
        long time2 = m2.getNanoTime();
        return Long.compare(time1, time2);
    }
}
