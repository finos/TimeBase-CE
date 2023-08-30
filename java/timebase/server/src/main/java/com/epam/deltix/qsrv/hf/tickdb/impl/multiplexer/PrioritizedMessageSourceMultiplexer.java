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
package com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer;


import com.epam.deltix.data.stream.RealTimeMessageSource;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.DebugFlags;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;
import com.epam.deltix.util.collections.generated.ObjectHashSet;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.concurrent.AbstractCursor;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableResource;
import com.epam.deltix.util.concurrent.NextResult;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 *  Merge multiple time-sorted message streams into one. Allows dynamic
 *  addition and removal of the source feeds.
 *
 * WARNING! This class should be updated only together with {@link PrioritizedMessageSourceMultiplexer_PDStream}.
 * After any change you should copy-paste code of modified methods to {@link PrioritizedMessageSourceMultiplexer_PDStream}.
 */
public class PrioritizedMessageSourceMultiplexer<T extends TimeStampedMessage>
    implements RealTimeMessageSource<T>
{
    public interface ExceptionHandler {
        /**
         *  Called when one of the multiplexed feeds throws an exception from the
         *  {@link AbstractCursor#next} method.
         *
         *  @param feed The feed that threw an exception.
         *  @param x    The exception.
         */
        void     nextThrewException(@Nonnull MessageSource<?> feed, @Nonnull RuntimeException x);
    }

    static final Log LOGGER = LogFactory.getLog("deltix.tickdb.msm");

    //
    //  Immutable properties
    //
    private final boolean                       ascending;
    //
    //  Behavior control properties
    //
    private boolean                             allowLateOutOfOrder = true;
    private ExceptionHandler                    handler = null;
    private boolean                             live = false;
    //
    //  Essentially final members, but set to null in close ()
    //
    protected MSPriorityQueue<T> queue;
    //
    //  Multiplexer state variables, guarded by "this"
    //
    private Runnable                            callerAvailLnr = null;
    protected PrioritizedSource<T>              currentSource = null;
    protected long                              currentTime = Long.MIN_VALUE;
    protected T                                 currentMessage = null;
    protected boolean                           isAtBeginning = true; // TODO: This flag is not used. Remove.
    private boolean                             isAtEnd = false;

    protected ObjectHashSet<PrioritizedSource<T>> emptySources = null;
    protected ArrayList<PrioritizedSource<T>>     checkSources = null;
    protected RuntimeException                    asyncException = null;

    // All sources here have type RealTimeMessageSource
    @Realtime
    private final ObjectHashSet<PrioritizedSource<T>> realTimeFeeds = new ObjectHashSet<>();

    protected T                                   realtimeMessage = null;
    protected boolean                             isRealTime = false;
    protected boolean                             realTimeStarted = false;
    protected final boolean                       realTimeNotification;
    //private volatile int                        nonRealTimeFeeds = 0;

    //private final ObjectToObjectHashMap<MessageSource <T>, String> last = new ObjectToObjectHashMap<MessageSource <T>, String>();

    private final ObjectToObjectHashMap<MessageSource<T>, PrioritizedSource<T>> wrapperBySource = new ObjectToObjectHashMap<>();

    static final class TimeComparator<T extends TimeStampedMessage> implements Comparator<T> {
        @Override
        public final int      compare(T o1, T o2) {
            long time1 = o1.getNanoTime();
            long time2 = o2.getNanoTime();

            // Unfortunately there is no easy way to simplify this. See Long.compare()
            return Long.compare(time1, time2);
        }
    }

    public PrioritizedMessageSourceMultiplexer() {
        this (true, false);
    }

    private PrioritizedMessageSourceMultiplexer(int capacity, boolean ascending, boolean realTimeNotification, Comparator<T> c) {
        this.ascending = ascending;
        this.realTimeNotification = realTimeNotification;
        this.queue = new MSPriorityQueue<>(capacity, ascending, c);
    }

    public PrioritizedMessageSourceMultiplexer(boolean ascending, boolean realTimeNotification, Comparator<T> c) {
        this(256, ascending, realTimeNotification, c);
    }

    public PrioritizedMessageSourceMultiplexer(boolean ascending, boolean realTimeNotification) {
        this(ascending, realTimeNotification, new TimeComparator<>());
    }

    @SuppressWarnings(value = {"unchecked", "varargs"})
    @SafeVarargs
    public PrioritizedMessageSourceMultiplexer(MessageSource<T>... feeds) {
        this(feeds.length, true, false, new TimeComparator<>());
        reset (feeds);
    }

    public PrioritizedMessageSourceMultiplexer(List <MessageSource <T>> feeds) {
        this(feeds.size(), true, false, new TimeComparator<>());
        reset (feeds);
    }

    public synchronized void        setAvailabilityListener (
        Runnable                        lnr
    )
    {
        callerAvailLnr = lnr;
    }

    public synchronized ExceptionHandler getExceptionHandler () {
        return handler;
    }

    public synchronized void        setExceptionHandler (ExceptionHandler handler) {
        this.handler = handler;
    }

    public synchronized boolean     isLive() {
        return live;
    }

    public synchronized void        setLive (boolean live) {
        this.live = live;
    }

    /**
     * If true, then MSM will expect real-time message (RealTimeStartMessage) from added sources and
     * will emit RealTimeStartMessage when all sources switched to the real-time mode only.
     * 
     * if true, all added messages sources expected to be {@link RealTimeMessageSource}
     * having realTimeAvailable() = true
     * 
     * @return true if MSM supports real-time mode.
     */

    @Override
    public boolean                  realTimeAvailable() {
        return realTimeNotification;
    }

    @Override
    public boolean                  isRealTime() {
        return isRealTime;
    }

    public synchronized boolean     isRealTimeStarted() {
        return isRealTime && realTimeStarted;
    }

    @SuppressWarnings("unchecked")
    public T                        createRealTimeMessage() {
        long time = currentTime != Long.MAX_VALUE ? currentTime : Long.MIN_VALUE;
        final RealTimeStartMessage msg = new RealTimeStartMessage();
        msg.setSymbol("");
        msg.setTimeStampMs(time);
        return (T) msg;             // Gene advised to always use native message. WAS: return null;  Bug#10343
    }
    
    public boolean                  isRealTimeMessage(T message) {
        if (message instanceof RawMessage)
            return ((RawMessage)message).type.getGuid().equals(RealTimeStartMessage.DESCRIPTOR_GUID);

        return message instanceof RealTimeStartMessage;
    }

    public synchronized boolean     getAllowLateOutOfOrder () {
        return allowLateOutOfOrder;
    }

    public synchronized void        setAllowLateOutOfOrder (boolean allowLateOutOfOrder) {
        this.allowLateOutOfOrder = allowLateOutOfOrder;
    }

    /**
     *  Removes the specified input feed from this multiplexer.
     *  If the cursor is not at beginning, and <b>current</b> feed is removed, 
     *  the current message becomes <code>null</code>. The cursor, however, is
     *  not advanced until {@link #next} is called.

     *  @param feed The feed to closeAndRemove.
     *  @return     If current message was removed.
     */
    public synchronized boolean     closeAndRemove (MessageSource <T> feed) {
        boolean     wasCurrent = remove(feed);

        feed.close();
        return (wasCurrent);
    }

    public synchronized boolean     remove (MessageSource <T> feed) {
        if (queue == null)
            return (false);

        PrioritizedSource<T> wrap = getWrap(feed);
        if (wrap == null) {
            return false;
        }

        boolean     wasCurrent = currentSource == wrap;

        uninstallListener (wrap);

        if (wasCurrent) {
            currentSource = null;
            currentMessage = null;
        }
        else if ((emptySources == null || !emptySources.remove(wrap)))
            queue.remove (wrap);

        if (checkSources != null)
            checkSources.remove(wrap);

        if (wrap.isRealtimeMessageSource())
            realTimeFeeds.remove(wrap);

        wrapperBySource.remove(feed);

        return (wasCurrent);
    }

    private void                    syncAddSourceNoNotify (PrioritizedSource <T> feed) {
        assert Thread.holdsLock (this);

        if (checkSources == null)
            checkSources = new ArrayList<> ();

        checkSources.add(feed);
    }

    private void                    addSourceAndNotify (PrioritizedSource <T> feed) {
        //assert !Thread.holdsLock (this);
        /* can't assert this because TickCursorImpl locks this (pretty much illegally) but
         * takes counter-measures by making the avlnr asynchronous...
         * the correct fix is to fix sync patterns of tick cursor impl not to use
         * mx as a mutex.
         */

        Runnable    lnr = null;

        synchronized (this) {
            syncAddSourceNoNotify (feed);
            lnr = syncNotify ();
        }

        if (lnr != null)
            lnr.run ();
    }

    /**
     *  Called from the availability listener installed in all multiplexed
     *  feeds.
     * @param feed message source to check
     */
    private void                    checkDataAvailable (PrioritizedSource <T> feed) {
        Runnable    lnr = null;

        synchronized (this) {
            if (emptySources != null && emptySources.remove (feed)) {
                syncAddSourceNoNotify (feed);
                lnr = syncNotify ();
            } else if (realTimeNotification) {
                // we waiting for this feed?
                if (feed.isRealtimeMessageSource() && realTimeFeeds.contains(feed))
                    notifyAll();
                else
                    lnr = syncNotify ();
            }
        }

        if (lnr != null)
            lnr.run ();
    }
    
    /**
     *  Adds a new feed without fast-forwarding it.
     *
     *  @param feed             The new feed
     */
    public void                     add (MessageSource <T> feed) {
        PrioritizedSource<T> wrap = wrapNew(feed);
        installListener (wrap);
        addSourceAndNotify (wrap);
    }

    /**
     * Same as {@link #add(MessageSource)} but with explicit source priority.
     */
    public void                     addWithPriority(MessageSource<T> feed, int priority) {
        PrioritizedSource<T> wrap = wrapNew(feed, priority);
        installListener (wrap);
        addSourceAndNotify (wrap);
    }

    /**
     *  Adds a new feed and fast-forwards it (rewinds if descending order),
     *  if necessary, until the specified timestamp, unless the
     *  allowLateOutOfOrder flag is not set. If the feed implements
     *  {@link IntermittentlyAvailableResource}, an availability listener is
     *  automatically installed in it.
     *
     *  @param feed              The new feed
     *  @param fastForwardToTime Scroll the feed to this timestamp.
     */
    public void                     add (MessageSource <T> feed, long fastForwardToTime) {
        Runnable        lnr;
        PrioritizedSource<T> wrap = wrapNew(feed);

        synchronized (this) {
            installListener (wrap);
            addInternal (wrap, fastForwardToTime);
            lnr = syncNotify ();
        }

        if (lnr != null)
            lnr.run ();
    }

    /**
     * Same as {@link #add(MessageSource, long)} but also explicitly assigns priority for a message source.
     *
     * Messages from a source with lower priority value are returned by {@link PrioritizedMessageSourceMultiplexer} before
     * messages from a source with higher priority value .
     */
    public void add(MessageSource<T> feed, long fastForwardToTime, int priority) {
        Runnable lnr;
        PrioritizedSource<T> wrap = wrapNew(feed, priority);

        synchronized (this) {
            installListener(wrap);
            addInternal(wrap, fastForwardToTime);
            lnr = syncNotify();
        }

        if (lnr != null)
            lnr.run();
    }

    protected final void                    closeFeed (PrioritizedSource <T> feed) {
        uninstallListener (feed);
        
        feed.getSrc().close ();
    }

    protected void                  addEmptySource(PrioritizedSource <T> feed) {
        if (emptySources == null)
            emptySources = new ObjectHashSet <> ();

        emptySources.add(feed);
    }

    /**
     * Returns true, if feed is closed.
     * @param feed Feed which generated exception
     * @param rtx Runtime exception to process
     * @return true, if feed is closed
     */
    protected final boolean                    handleException (
        PrioritizedSource<T>            feed,
        RuntimeException                rtx
    )
    {
        if (rtx.getClass () == UnavailableResourceException.class) {
            addEmptySource(feed);

            return false;
        }
        else if (handler == null) {
            asyncException = rtx;
            closeFeed (feed);

            return true;
        }
        else {
            try {
                handler.nextThrewException (feed.getSrc(), rtx);
                return false;
            } catch (RuntimeException x) {
                asyncException = x;
                closeFeed (feed);

                return true;
            }
        }
    }

    protected NextResult                 nextAvailable (@Realtime PrioritizedSource<T> feed) {
        assert feed.isRealtimeMessageSource();

        for(;;) {
            synchronized (this) {

                if (queue == null)
                    throw new CursorIsClosedException ();
                
                NextResult next = moveNext (feed, false);

                if (next == NextResult.UNAVAILABLE) {
                    realTimeFeeds.add(feed);

                    try {
                        wait ();
                        continue;
                    } catch (InterruptedException e) {
                        throw new UncheckedInterruptedException (e);
                    }
                } else if (next == NextResult.OK && feed.getRealtimeSrc().isRealTime()) {

                    if (LOGGER.isEnabled(LogLevel.DEBUG))
                        LOGGER.debug(this + ": received real-time message " + feed.getMessage() + " from " + feed.getSrc());

                    boolean hasReadTime = isRealTime;
                    if (realTimeFeeds.remove(feed) && verifyRealTimeMode()) {
                        if (!hasReadTime) {
                            if (LOGGER.isEnabled(LogLevel.DEBUG))
                                LOGGER.debug(this + " switched to real-time");

                            realtimeMessage = feed.getMessage();
                        }
                    }
                }

                return next;
            }
        }
    }

    protected NextResult moveNext(PrioritizedSource<T> feed, boolean addEmpty) {
        try {
            if (feed.getSrc().next ()) {
                return NextResult.OK;
            } else {
                closeFeed (feed);
                return NextResult.END_OF_CURSOR;
            }
        } catch (UnavailableResourceException x) {
            if (addEmpty)
                addEmptySource(feed);

            return NextResult.UNAVAILABLE;
        } catch (RuntimeException x) {
            if (handleException (feed, x))
                return NextResult.END_OF_CURSOR;

            return null;
        }
    }

    private NextResult advance (PrioritizedSource<T> feed) {
        if (realTimeNotification) {

            if (feed.isRealtimeMessageSource()) {
                NextResult result = advanceRealTime(feed);
                if (result != null)
                    return result;
            }
        }
        
       return moveNext(feed, true);
    }

    protected final NextResult advanceRealTime(@Realtime PrioritizedSource<T> source) {
        //  if feed still not in real-time mode - advance with wait
        //  and check that source switched to the real-time mode
        RealTimeMessageSource<T> realtimeSrc = source.getRealtimeSrc();
        if (realtimeSrc.realTimeAvailable()) {
            if (!realtimeSrc.isRealTime()) {

                // add source into waiting feeds
                //if (!realTimeFeeds.contains(source))
                //    realTimeFeeds.add(source);

                if (isRealTime) {
                    if (LOGGER.isEnabled(LogLevel.DEBUG))
                        LOGGER.debug(this + " advance() set " + source.getSrc() + " in real-time = false");
                    isRealTime = false;
                }

                NextResult result = nextAvailable(source);
                if (result != NextResult.OK)
                    return result;
                else if (!realtimeSrc.isRealTime())
                    return NextResult.OK;
            }

            // we should skip any RTSM in next
            for (;;) {
                NextResult result = moveNext(source, true);
                if (result == NextResult.OK) {
                    if (!isRealTimeMessage(source.getMessage()))
                        return NextResult.OK;
                } else {
                    return result;
                }
            }
        }
        // Fallback to the default flow
        return null;
    }

    @SuppressWarnings ("unchecked")
    private void                    installListener (final PrioritizedSource <T> feed) {
        if (feed.getSrc() instanceof IntermittentlyAvailableResource)
            ((IntermittentlyAvailableResource) feed.getSrc()).setAvailabilityListener (
                new Runnable () {
                    public void     run () {
                        checkDataAvailable (feed);                
                    }
                }
            );

        boolean supportsRealTime = false;

        if (realTimeNotification && feed.isRealtimeMessageSource()) {
            RealTimeMessageSource<T> source = feed.getRealtimeSrc();

            supportsRealTime = source.realTimeAvailable();
//            if (!supportsRealTime)
//                throw new IllegalArgumentException ("When realtime notifications is enabled all sources must support it.");

            if (supportsRealTime && !source.isRealTime()) {
                isRealTime = false;
                if (LOGGER.isEnabled(LogLevel.DEBUG)) {
                    LOGGER.debug(this + " installListener() set " + source + " in real-time = false");
                }
                if (!realTimeFeeds.contains(feed)) {
                    realTimeFeeds.add(feed);
                }
            }
        }

//        if (!supportsRealTime)
//            nonRealTimeFeeds++;
    }

    private boolean                 verifyRealTimeMode() {

        if (isRealTime)
            return true;

        isRealTime = realTimeNotification && realTimeFeeds.isEmpty(); // && nonRealTimeFeeds == 0;

        if (!isRealTime && LOGGER.isEnabled(LogLevel.DEBUG))
            LOGGER.debug(this + " verifyRealTimeMode() set real-time = false");

        return isRealTime;
    }

    private void                    uninstallListener (PrioritizedSource <T> feed) {
        if (feed.getSrc() instanceof IntermittentlyAvailableResource)
            ((IntermittentlyAvailableResource) feed.getSrc()).setAvailabilityListener (null);

        if (realTimeNotification)
            if (feed.isRealtimeMessageSource() && feed.getRealtimeSrc().realTimeAvailable()) {
                if (realTimeFeeds.remove(feed))
                    verifyRealTimeMode();
            }
//            else {
//                nonRealTimeFeeds--;
//            }
    }

    private Runnable                syncNotify () {
        assert Thread.holdsLock (this);

        if (callerAvailLnr == null) {
            notifyAll ();
            return (null);
        }
        else
            return (callerAvailLnr);
    }

    private void                    addInternal (PrioritizedSource<T> feed, long fastForwardToTime) {

        while (advance (feed) == NextResult.OK) {
            if (!allowLateOutOfOrder) {
                TimeStampedMessage  msg = feed.getMessage ();
                long                ts = msg.getTimeStampMs ();

                if (ascending ? ts < fastForwardToTime : ts > fastForwardToTime) {
                    processDiscardedMessage(fastForwardToTime, msg);

                    continue;
                }
            }

            queue.offer (feed);
            break;
        }
    }

    private void processDiscardedMessage(long fastForwardToTime, TimeStampedMessage msg) {
        if (DebugFlags.DEBUG_MSG_DISCARD) {
            DebugFlags.discard (
                "TB DEBUG: Discarding " + msg +
                " while fast-forwarding to " + fastForwardToTime
            );
        }
    }

    protected final void addSync(PrioritizedSource<T> feed, long fastForwardToTime) {
        addInternal(feed, fastForwardToTime);

        if (asyncException != null)
            throw asyncException;
    }

    private void addSync(PrioritizedSource<T> feed) {
        if (advance (feed) == NextResult.OK)
            queue.offer (feed);
        else if (asyncException != null)
            throw asyncException;
    }

    public synchronized void        clearSources () {
        clearSourcesInternal ();
    }

    private void                    clearSourcesInternal () {
        if (currentSource != null)
            currentSource.getSrc().close ();

        while (!queue.isEmpty ())
            queue.poll().getSrc().close();

        if (emptySources != null) {
            for (PrioritizedSource<T> feed : emptySources)
                closeFeed (feed);

            emptySources.clear ();
        }

        if (checkSources != null) {
            for (PrioritizedSource<T> feed : checkSources)
                closeFeed (feed);

            checkSources.clear ();
        }

        // real-time related fields
        realTimeFeeds.clear();

        wrapperBySource.clear();

        currentSource = null;
        currentMessage = null;
        isAtBeginning = true;
        isAtEnd = false;

        isRealTime = false;
        realtimeMessage = null;
        //nonRealTimeFeeds = 0;

        currentTime = Long.MIN_VALUE;
    }

    /**
     *  Call {@link #clearSources}, and then add the specified feeds.
     *
     *  @param feeds        The feeds to add.
     */
    @SuppressWarnings(value = {"unchecked", "varargs"})
    public void                 reset (MessageSource <T> ... feeds) {
        Runnable        lnr;

        synchronized (this) {
            clearSourcesInternal ();

            for (MessageSource <T> feed : feeds) {
                PrioritizedSource<T> wrap = getOrCreateWrap(feed);
                installListener(wrap);
                syncAddSourceNoNotify(wrap);
            }

            lnr = syncNotify ();
        }

        if (lnr != null)
            lnr.run ();
    }
        
    /**
     *  Call {@link #clearSources}, and then add the specified feeds.
     *
     *  @param feeds        The feeds to add.
     */
    public void                     reset (Collection <MessageSource <T>> feeds) {
        Runnable        lnr;

        synchronized (this) {
            clearSourcesInternal ();

            for (MessageSource <T> feed : feeds) {
                PrioritizedSource<T> wrap = getOrCreateWrap(feed);
                installListener(wrap);
                syncAddSourceNoNotify(wrap);
            }

            lnr = syncNotify ();
        }

        if (lnr != null)
            lnr.run ();
    }
        
    /**
     *  Call {@link #clearSources}, and then add the specified feeds, fast-forwarding
     *  each until the timestamp of the current message from the feed is 
     *  at least <code>startTime</code>.
     * 
     *  @param startTime        Fast-forward to this time.
     *  @param feeds            The feeds to add.
     */
    @SuppressWarnings(value = {"unchecked", "varargs"})
    public void                 reset (long startTime, MessageSource <T> ... feeds) {
        Runnable        lnr;

        synchronized (this) {
            clearSourcesInternal ();

            currentTime = startTime;

            for (MessageSource <T> feed : feeds) {
                PrioritizedSource<T> wrap = getOrCreateWrap(feed);
                installListener(wrap);
                addInternal(wrap, startTime);
            }

            lnr = syncNotify ();
        }

        if (lnr != null)
            lnr.run ();
    }

    /**
     *  Call {@link #clearSources}, and set fast-forward time to the specified
     *  value.
     *
     *  @param startTime        Fast-forward to this time.
     */
    public synchronized void        reset (long startTime) {
        clearSourcesInternal ();
        currentTime = startTime;
    }

    /**
     *  Call {@link #clearSources}, and then add the specified feeds, fast-forwarding
     *  each until the timestamp of the current message from the feed is 
     *  at least <code>startTime</code>.
     * 
     *  @param startTime        Fast-forward to this time.
     *  @param feeds        The feeds to add.
     */
    public void                     reset (long startTime, Collection <MessageSource <T>> feeds) {
        Runnable        lnr;

        synchronized (this) {
            clearSourcesInternal ();

            currentTime = startTime;

            for (MessageSource <T> feed : feeds) {
                PrioritizedSource<T> wrap = getOrCreateWrap(feed);
                installListener(wrap);
                addInternal(wrap, startTime);
            }

            lnr = syncNotify ();
        }

        if (lnr != null)
            lnr.run ();
    }

    public synchronized void        invalidateRealTimeState(RealTimeMessageSource<T> source) {
        @Realtime PrioritizedSource<T> wrap = getOrCreateWrap(source);
        if (isRealTime)
            isRealTime = source.isRealTime();

        if (!source.isRealTime()) {
            if (emptySources != null && emptySources.contains(wrap)) {
                emptySources.remove(wrap);

                if (!checkSources.contains(wrap))
                    checkSources.add(wrap);
            }

            if (!realTimeFeeds.contains(wrap))
                realTimeFeeds.add(wrap);
        }
    }

    /**
     *  Asynchronous method which will cause next () to throw the specified 
     *  exception.
     *
     *  @param x
     */
    public void                     setException (RuntimeException x) {
        Runnable        lnr;

        synchronized (this) {
            asyncException = x;
            lnr = syncNotify ();
        }

        if (lnr != null)
            lnr.run ();
    }

    @Override
    public synchronized boolean     next () {
        return (syncNext ());
    }

    public boolean                  syncNext () {
        return syncNext(true) == NextResult.OK;
    }

    protected NextResult                  syncNext (boolean throwable) {
        assert Thread.holdsLock (this);
        
        for (;;) {
            //
            //  This checks for feed exception caught prior to next (), or
            //  asynchronously in the availability listener while this thread
            //  was in wait ().
            //
            if (asyncException != null)
                throw asyncException;

            if (queue == null)
                throw new CursorIsClosedException ();
            //
            //  Re-offer last used feed
            //
            if (isAtBeginning) {
                isAtBeginning = false;
            } else if (currentSource != null) {
                // Main path for historic data
                addSync(currentSource);
            }

            currentSource = null;
            currentMessage = null;
            
            realTimeStarted = false;
            //
            //  Re-check newly available sources
            //
            if (checkSources != null) {
                // TODO: Extract this loop into separate method
                for (;;) {
                    int         n = checkSources.size ();

                    if (n == 0)
                        break;

                    addSync (checkSources.remove (n - 1), currentTime);                    
                }
            }

            if (queue.isEmpty ()) {
                NextResult x = processEmptyQueue(throwable);
                if (x != null) {
                    return x;
                }
            }
            else {
                if (isRealTime && realtimeMessage != null) {
                    sendRealTimeMessage(" send real-time message: ");
                }
                else {
                    // Main path for historic data
                    currentSource = queue.poll();

//                    String previous = last.get(currentSource, null);
//                    if (previous != null && previous.equals(currentSource.getMessage().toString())) {
//                        System.out.println("------------" + this + ": Found possible dub from: " +  currentSource  + " = " + currentSource.getMessage());
//                    }
//                    last.put(currentSource, currentMessage.toString());

                    currentMessage = currentSource.getMessage();
                    currentTime = currentMessage.getTimeStampMs();
                }

                return NextResult.OK;
            }            
        }      
    }

    protected final NextResult processEmptyQueue(boolean throwable) {
        assert Thread.holdsLock (this);

        boolean isEmpty = isEmpty();

        if (!live && isEmpty) {
            isAtEnd = true;
            return NextResult.END_OF_CURSOR;
        }
        else if (realtimeMessage != null) {

            assert isRealTimeMessage(realtimeMessage);
            assert (isRealTime);

            sendRealTimeMessage(" send real-time message: ");
            return NextResult.OK;
        }
        else if (realTimeNotification && isEmpty && !isRealTime) {
            isRealTime = true;
            sendRealTimeMessage(" send new real-time message: ");
            return NextResult.OK;
        }
        else if (callerAvailLnr != null) {
            if (throwable)
                throw UnavailableResourceException.INSTANCE;

            return NextResult.UNAVAILABLE;
        } else {
            try {
                wait ();
                return null;
            } catch (InterruptedException x) {
                throw new UncheckedInterruptedException(x);
            }
        }
    }

    protected final void sendRealTimeMessage(String logText) {
        currentMessage = createRealTimeMessage();
        currentSource = null;
        realtimeMessage = null;
        realTimeStarted = true;

        if (LOGGER.isEnabled(LogLevel.DEBUG))
            LOGGER.debug(this + logText + currentMessage);
    }

    protected boolean              isEmpty() {
        return emptySources == null || emptySources.isEmpty();
    }

    public final synchronized boolean isAtEnd () {
        return (isAtEnd);
    }

    public final synchronized T     getMessage () {
        return (currentMessage);        
    }

    public final synchronized long  getCurrentTime () {
        return (currentTime);
    }
    
    public synchronized MessageSource <T> getCurrentSource () {
        return currentSource.getSrc();
    }

    public synchronized boolean     isClosed () {
        return (queue == null);
    }

    public final boolean            syncIsAtEnd () {
        assert Thread.holdsLock (this);
        return (isAtEnd);
    }

    public final T                  syncGetMessage () {
        assert Thread.holdsLock (this);
        return (currentMessage);
    }

    public final long               syncGetCurrentTime () {
        assert Thread.holdsLock (this);
        return (currentTime);
    }

    public MessageSource <T>        syncGetCurrentSource () {
        assert Thread.holdsLock (this);
        return currentSource.getSrc();
    }

    public boolean                  syncIsClosed () {
        assert Thread.holdsLock (this);
        return (queue == null);
    }

    public void                     close () {
        Runnable        lnr;

        synchronized (this) {
            if (queue == null)
                return;

            //  Close everything
            clearSourcesInternal ();

            queue = null;
            emptySources = null;

            lnr = syncNotify ();
        }

        if (lnr != null)
            lnr.run ();
    }

    private PrioritizedSource<T> wrapNew(MessageSource<T> feed) {
        return wrapNew(feed, 0);
    }

    private PrioritizedSource<T> wrapNew(MessageSource<T> feed, int priority) {
        PrioritizedSource<T> wrapper = new PrioritizedSource<>(feed, priority);
        wrapperBySource.put(feed, wrapper);
        return wrapper;
    }

    @Nullable
    private PrioritizedSource<T> getWrap(MessageSource<T> feed) {
        return wrapperBySource.get(feed, null);
    }

    @Nonnull
    private PrioritizedSource<T> getOrCreateWrap(MessageSource<T> feed) {
        PrioritizedSource<T> wrapper = wrapperBySource.get(feed, null);
        if (wrapper == null) {
            wrapper = wrapNew(feed);
        }
        return wrapper;
    }

    /**
     * Marks a source that it's realtime source (implements {@link RealTimeMessageSource}).
     */
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface Realtime {
    }
}