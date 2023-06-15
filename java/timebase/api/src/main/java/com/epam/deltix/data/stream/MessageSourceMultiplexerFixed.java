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
package com.epam.deltix.data.stream;

import com.google.common.annotations.VisibleForTesting;
import com.epam.deltix.data.stream.pq.PriorityQueueExt;
import com.epam.deltix.qsrv.hf.tickdb.impl.DebugFlags;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.collections.generated.ObjectHashSet;
import com.epam.deltix.util.concurrent.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.List;

/**
 * Alternative implementation of {@link MessageSourceMultiplexer} that is expected to provide better performance but has a set of limitations:
 * <ul>
 *     <li>MAIN: it's impossible to change subscription on the fly</li>
 *     <li>no realTimeNotification option</li>
 *     <li>allowLateOutOfOrder option is static (however this can be changed in cost of minor penalty to performance)</li>
 * </ul>
 */
@ParametersAreNonnullByDefault
public final class MessageSourceMultiplexerFixed<T extends TimeStampedMessage> {

    //private static final Logger LOGGER = Logger.getLogger("deltix.tickdb.msm");

    private final MessageSourceMultiplexer.ExceptionHandler handler;
    private final byte direction;
    private final PriorityQueueExt<MessageSource<T>> queue;
    private final ArrayDeque<MessageSource<T>> checkSources;
    private ObjectHashSet<MessageSource<T>> emptySources = null;

    private static final boolean allowLateOutOfOrder = true;
    private boolean isAtEnd = false;
    private boolean isClosed = false;

    private long currentTime;
    private final Object lock;
    private Runnable callerAvailLnr = null;


    protected MessageSource<T> currentSource = null;
    protected T currentMessage = null;

    // Exception handling
    private RuntimeException asyncException = null;

    public MessageSourceMultiplexerFixed(MessageSourceMultiplexer.ExceptionHandler xhandler, List<MessageSource<T>> messageSources, boolean ascending, long currentTime, Object lock) {
        this.handler = xhandler;
        this.direction = (byte) (ascending ? 1 : -1);
        this.currentTime = currentTime;
        this.lock = lock;
        this.queue = new PriorityQueueExt<>(messageSources.size(), ascending); // Queue is empty initially
        this.checkSources = new ArrayDeque<>(messageSources);
        for (MessageSource<T> source : messageSources) {
            installListener(source);
        }
    }

    private void syncAddSourceNoNotify(MessageSource<T> feed) {
        assert Thread.holdsLock(lock);
        checkSources.add(feed);
    }


    /**
     * Called from the availability listener installed in all multiplexed
     * feeds.
     *
     * @param feed message source to check
     */
    private void checkDataAvailable(MessageSource<T> feed) {
        Runnable lnr = null;

        synchronized (lock) {
            if (emptySources != null && emptySources.remove(feed)) {
                syncAddSourceNoNotify(feed);
                lnr = syncNotify();
            }
        }

        if (lnr != null)
            lnr.run();
    }

    protected final void closeFeed(MessageSource<T> feed) {
        uninstallListener(feed);
        feed.close();
    }

    protected void addEmptySource(MessageSource<T> feed) {
        if (emptySources == null) {
            emptySources = new ObjectHashSet<>();
        }
        emptySources.add(feed);
    }

    /**
     * Returns true, if feed is closed.
     *
     * @param feed
     * @param rtx
     * @return true, if feed is closed
     */
    protected final boolean handleException(
            MessageSource<T> feed,
            RuntimeException rtx
    ) {
        if (rtx.getClass() == UnavailableResourceException.class) {
            addEmptySource(feed);

            return false;
        } else if (handler == null) {
            asyncException = rtx;
            closeFeed(feed);

            return true;
        } else {
            try {
                handler.nextThrewException(feed, rtx);
                return false;
            } catch (RuntimeException x) {
                asyncException = x;
                closeFeed(feed);

                return true;
            }
        }
    }

    protected NextResult moveNext(MessageSource<T> feed, boolean addEmpty) {
        try {
            if (feed.next()) {
                return NextResult.OK;
            } else {
                closeFeed(feed);
                return NextResult.END_OF_CURSOR;
            }
        } catch (UnavailableResourceException x) {
            if (addEmpty) {
                addEmptySource(feed);
            }

            return NextResult.UNAVAILABLE;
        } catch (RuntimeException x) {
            if (handleException(feed, x)) {
                return NextResult.END_OF_CURSOR;
            }

            return null;
        }
    }

    private NextResult advance(MessageSource<T> feed) {
        return moveNext(feed, true);
    }

    private void installListener(final MessageSource<T> feed) {
        if (feed instanceof IntermittentlyAvailableResource)
            ((IntermittentlyAvailableResource) feed).setAvailabilityListener(
                    new Runnable() {
                        public void run() {
                            checkDataAvailable(feed);
                        }
                    }
            );
    }

    private void uninstallListener(MessageSource<T> feed) {
        if (feed instanceof IntermittentlyAvailableResource) {
            ((IntermittentlyAvailableResource) feed).setAvailabilityListener(null);
        }
    }

    private Runnable syncNotify() {
        assert Thread.holdsLock(lock);

        if (callerAvailLnr == null) {
            lock.notifyAll();
            return (null);
        } else {
            return (callerAvailLnr);
        }
    }

    private void addInternal(MessageSource<T> feed, long fastForwardToTime) {

        while (advance(feed) == NextResult.OK) {
            if (!allowLateOutOfOrder) {
                TimeStampedMessage msg = feed.getMessage();
                long ts = msg.getTimeStampMs();

                if ((ts - fastForwardToTime) * direction < 0) {
                    processDiscardedMessage(fastForwardToTime, msg);
                    continue;
                }
            }

            addToPriorityQueue(feed);
            break;
        }
    }

    private void processDiscardedMessage(long fastForwardToTime, TimeStampedMessage msg) {
        if (DebugFlags.DEBUG_MSG_DISCARD) {
            DebugFlags.discard(
                    "TB DEBUG: Discarding " + msg +
                            " while fast-forwarding to " + fastForwardToTime
            );
        }
    }


    private void addSync(MessageSource<T> feed, long fastForwardToTime) {
        addInternal(feed, fastForwardToTime);

        if (asyncException != null) {
            throw asyncException;
        }
    }

    private void addSync(MessageSource<T> feed) {
        if (advance(feed) == NextResult.OK) {
            addToPriorityQueue(feed);
        } else if (asyncException != null) {
            throw asyncException;
        }
    }

    private void addToPriorityQueue(MessageSource<T> feed) {
        queue.offer(feed, feed.getMessage().getNanoTime());
    }

    @VisibleForTesting
    public boolean next() {
        synchronized (lock) {
            return syncNextInternal() == NextResult.OK;
        }
    }

    public boolean syncNext() {
        assert Thread.holdsLock(lock);
        return syncNextInternal() == NextResult.OK;
    }

    private NextResult syncNextInternal() {
        boolean throwable = true;

        while (true) {
            //
            //  This checks for feed exception caught prior to next (), or
            //  asynchronously in the availability listener while this thread
            //  was in wait ().
            //
            if (asyncException != null) {
                throw asyncException;
            }

            if (queue == null) {
                throw new CursorIsClosedException();
            }
            //
            //  Re-offer last used feed
            //
            if (currentSource != null) {
                // Main path for historic data
                addSync(currentSource);
                currentSource = null;
            }

            currentMessage = null;
            //
            //  Re-check newly available sources
            //
            if (checkSources != null && !checkSources.isEmpty()) {
                processCheckSources();
            }

            if (queue.isEmpty()) {
                NextResult x = processEmptyQueue(throwable);
                if (x != null) {
                    return x;
                }
            } else {
                // Main path
                currentSource = queue.poll();
                assert currentSource != null;
                currentMessage = currentSource.getMessage();
                currentTime = currentMessage.getTimeStampMs();

                return NextResult.OK;
            }
        }
    }

    private void processCheckSources() {
        MessageSource<T> src;
        while ((src = checkSources.poll()) != null) {
            addSync(src, currentTime);
        }
    }

    private NextResult processEmptyQueue(boolean throwable) {
        boolean isEmpty = isEmpty();

        if (isEmpty) {
            isAtEnd = true;
            return NextResult.END_OF_CURSOR;
        } else if (callerAvailLnr != null) {
            if (throwable) {
                throw UnavailableResourceException.INSTANCE;
            }
            return NextResult.UNAVAILABLE;
        } else {
            try {
                lock.wait();
                return null;
            } catch (InterruptedException x) {
                throw new UncheckedInterruptedException(x);
            }
        }
    }

    private boolean isEmpty() {
        return emptySources == null || emptySources.isEmpty();
    }

    public void setAvailabilityListener(@Nullable Runnable lnr) {
        synchronized (lock) {
            callerAvailLnr = lnr;
        }
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public T syncGetMessage() {
        assert Thread.holdsLock(lock);
        return currentMessage;
    }

    public MessageSource<T> syncGetCurrentSource() {
        assert Thread.holdsLock(lock);
        return currentSource;
    }


    public boolean syncIsAtEnd() {
        assert Thread.holdsLock(lock);
        return isAtEnd;
    }

    public boolean syncIsClosed() {
        assert Thread.holdsLock(lock);
        return isClosed;
    }

    private void clearSourcesInternal() {
        if (currentSource != null) {
            currentSource.close();
        }

        while (!queue.isEmpty()) {
            queue.poll().close();
        }

        if (emptySources != null) {
            for (MessageSource<T> feed : emptySources) {
                closeFeed(feed);
            }

            emptySources.clear();
        }

        if (checkSources != null) {
            for (MessageSource<T> feed : checkSources) {
                closeFeed(feed);
            }
            checkSources.clear();
        }

        currentSource = null;
        currentMessage = null;
        isAtEnd = false;

        currentTime = Long.MIN_VALUE;
    }

    public void close() {
        Runnable lnr;

        synchronized (lock) {
            if (isClosed) {
                return;
            }

            //  Close everything
            clearSourcesInternal();
            emptySources = null;
            isClosed = true;

            lnr = syncNotify();
        }

        if (lnr != null) {
            lnr.run();
        }
    }
}