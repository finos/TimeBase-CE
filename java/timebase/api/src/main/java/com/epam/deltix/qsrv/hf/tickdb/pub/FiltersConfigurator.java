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

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.blocks.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

public final class FiltersConfigurator {

    private static BumpUpSorter newBumpUpSorter(MessageChannel<InstrumentMessage> channel,
                                                TickLoader loader,
                                                long maxDiscrepancy)
    {
        DXTickStream stream = ((DXTickStream)loader.getTargetStream());

        TimeIdentity id = stream.getDistributionFactor() == 0 ?
                new TimeIdentitySet<TimeEntry>(new TimeEntry()) : new TimeEntry();

        BumpUpSorter sorter = new BumpUpSorter(id, channel, maxDiscrepancy);
        sorter.setIgnoreErrors(false);
        sorter.setName(stream.getKey());
        if (loader instanceof LoadingErrorListener)
            sorter.setListener((LoadingErrorListener)loader);

        return sorter;
    }
    
    public static BumpUpSorter  newBumpUpSorter(TickLoader loader, long maxDiscrepancy) {
       return newBumpUpSorter(loader, loader, maxDiscrepancy);
    }

    public static BumpUpSorter  newBumpUpSorter(MessageChannel<InstrumentMessage> previous,
                                                long maxDiscrepancy) {
       return newBumpUpSorter(previous, getLoader(previous), maxDiscrepancy);
    }    

    private static SkipSorter newSkipSorter(MessageChannel<InstrumentMessage> channel,
                                                TickLoader loader,
                                                long maxDiscrepancy)
    {
        DXTickStream stream = ((DXTickStream)loader.getTargetStream());
        
        TimeIdentity id = stream.getDistributionFactor() == 0 ?
                new TimeIdentitySet<TimeEntry>(new TimeEntry()) : new TimeEntry();

        SkipSorter sorter = new SkipSorter(id, channel, maxDiscrepancy);

        sorter.setIgnoreErrors(false);
        sorter.setName(stream.getKey());
        if (loader instanceof LoadingErrorListener)
            sorter.setListener((LoadingErrorListener)loader);

        return sorter;
    }

    public static SkipSorter   newSkipSorter(TickLoader loader, long maxDiscrepancy) {
       return newSkipSorter(loader, loader, maxDiscrepancy);
    }

    public static SkipSorter    newSkipSorter(MessageChannel<InstrumentMessage> previous,
                                              long maxDiscrepancy) {
       return newSkipSorter(previous, getLoader(previous), maxDiscrepancy);
    }

    private static BufferedSorter newBufferedSorter(MessageChannel<InstrumentMessage> channel,
                                                TickLoader loader,
                                                long maxDiscrepancy)
    {
        DXTickStream stream = ((DXTickStream)loader.getTargetStream());
        BufferedSorter.Entry e = new BufferedSorter.Entry();
        BufferedSorter sorter = stream.getDistributionFactor() == 0 ? 
                new BufferedSorter(new TimeIdentitySet<BufferedSorter.Entry>(e), channel, maxDiscrepancy) :
                new BufferedSorter(e, channel, maxDiscrepancy);
        
        sorter.setIgnoreErrors(false);
        sorter.setName(stream.getKey());
        if (loader instanceof LoadingErrorListener)
            sorter.setListener((LoadingErrorListener)loader);

        return sorter;
    }

    public static BufferedSorter   newBufferedSorter(TickLoader loader, long maxDiscrepancy) {
       return newBufferedSorter(loader, loader, maxDiscrepancy);
    }

    public static BufferedSorter    newBufferedSorter(MessageChannel<InstrumentMessage> previous, long maxDiscrepancy) {
       return newBufferedSorter(previous, getLoader(previous), maxDiscrepancy);
    }

    private static TickLoader   getLoader(MessageChannel from) {
        if (from instanceof AbstractSorter) {
            
            MessageChannel channel = ((AbstractSorter)from).getChannel();
            if (channel instanceof TickLoader)
                return (TickLoader)channel;

            return getLoader(channel);
                        
        } else if (from instanceof TickLoader) {
            return (TickLoader)from; 
        }

        return null;
    }

    private static MessageChannel<InstrumentMessage>       createChannel(
            MessageChannel<InstrumentMessage> channel,
            long bufferedInterval,
            long bumpInterval,
            long skipInterval,
            int index)
    {
        if (index == 1 && bufferedInterval > 0)
            return newBufferedSorter(channel, bufferedInterval);
        else if (index == 2 && bumpInterval > 0)
            return newBumpUpSorter(channel, bumpInterval);
        else if (index == 3 && skipInterval > 0)
            return newSkipSorter(channel, skipInterval);

        return channel;
    }

    public static MessageChannel<InstrumentMessage> create(TickLoader loader,
                                                           long bufferedInterval,
                                                           long bumpInterval,
                                                           long skipInterval)
    {
        int[] order = new int[] {0, 0, 0};
        long max;

        // sorters priority - buffered, bumpup, skip
        if (bufferedInterval >= (max = Math.max(bumpInterval, skipInterval)) && bufferedInterval > 0) {
            order[0] = 1;
            if (bufferedInterval > max) {
                order[1] = bumpInterval >= skipInterval ? 2 : 3;
                order[2] = bumpInterval >= skipInterval ? 3 : 2;
            }
        } else if (bumpInterval >= (max = Math.max(bufferedInterval, skipInterval)) && bumpInterval > 0) {
            order[0] = 2;
            if (bumpInterval > max) {
                order[1] = bufferedInterval >= skipInterval ? 1 : 3;
                order[2] = bufferedInterval >= skipInterval ? 3 : 1;
            }
        } else if (skipInterval > Math.max(bufferedInterval, bumpInterval) && skipInterval > 0) {
            order[0] = 3;
            order[1] = bufferedInterval >= bumpInterval ? 1 : 2;
            order[2] = bufferedInterval >= bumpInterval ? 2 : 1;
        }

        MessageChannel<InstrumentMessage> channel = loader;
        for (int index : order)
            channel = createChannel(channel, bufferedInterval, bumpInterval, skipInterval, index);

        return channel;
    }
}