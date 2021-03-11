package com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer;

import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.concurrent.NextResult;
import com.epam.deltix.util.concurrent.UnavailableResourceException;

import java.util.Comparator;

/**
 * See also docs for {@link deltix.qsrv.hf.tickdb.impl.TickCursorImpl_PDStream}.
 *
 * @author Alexei Osipov
 */
public class PrioritizedMessageSourceMultiplexer_PDStream<T extends TimeStampedMessage> extends PrioritizedMessageSourceMultiplexer<T> {
    public PrioritizedMessageSourceMultiplexer_PDStream(boolean ascending, boolean realTimeNotification, Comparator<T> c) {
        super(ascending, realTimeNotification, c);
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

    private void addSync(PrioritizedSource<T> feed) {
        if (advance (feed) == NextResult.OK)
            queue.offer (feed);
        else if (asyncException != null)
            throw asyncException;
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
                throw new CursorIsClosedException();
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
}
