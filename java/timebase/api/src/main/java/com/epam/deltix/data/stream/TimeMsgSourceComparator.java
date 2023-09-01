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
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.lang.MathUtil;

import java.util.Comparator;

/**
 *
 */
public class TimeMsgSourceComparator 
    implements Comparator<MessageSource<? extends TimeStampedMessage>>
{
    private TimeMsgSourceComparator () { }
    
    public static final Comparator <MessageSource <? extends TimeStampedMessage>> INSTANCE =
        new TimeMsgSourceComparator ();
    
    public int      compare (
        MessageSource <? extends TimeStampedMessage> o1,
        MessageSource <? extends TimeStampedMessage> o2
    )
    {
        long            ts1 = o1.getMessage().getNanoTime();
        long            ts2 = o2.getMessage().getNanoTime();
        
        return (MathUtil.sign (ts1 - ts2));
    }
}