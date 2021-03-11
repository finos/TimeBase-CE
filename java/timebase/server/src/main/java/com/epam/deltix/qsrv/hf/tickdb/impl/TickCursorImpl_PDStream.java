package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer.PrioritizedMessageSourceMultiplexer_PDStream;
import com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer.PrioritizedSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.TypedMessageSource;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;

/**
 * This class is workaround for the problem with Profile Pollution for {@link TickCursorImpl}.
 * It tricks Java JIT into creating a separate execution profile for {@link TickCursorImpl#next} by creation of method body copy.
 * Note: just calling {@code super.next()} does not work in this case.
 *
 * <p>See <a href="https://wiki.openjdk.java.net/display/HotSpot/MethodData#MethodData-ProfilepollutionPollution">Profile pollution</a>.</p>
 *
 * <p><a href="http://stackoverflow.com/questions/30687717/java-8-stream-unpredictable-performance-drop-with-no-obvious-reason">
 *     Similar problem</a></p>
 *
 * Relevant JDK bug: <a href="https://bugs.openjdk.java.net/browse/JDK-8015416">JDK-8015416: Collect context-dependent split profiles</a>.
 */
class TickCursorImpl_PDStream extends TickCursorImpl {

    TickCursorImpl_PDStream(TickDBImpl db, SelectionOptions options, TickStream... streams) {
        super(db, getOpts(options), streams);
    }

    private static ConstructorOptions getOpts(SelectionOptions options) {
        if (options == null)
            options = new SelectionOptions ();
        CursorMultiplexer cursorMultiplexer = new CursorMultiplexer(options);
        return new ConstructorOptions(options, cursorMultiplexer);
    }

    private static class CursorMultiplexer extends PrioritizedMessageSourceMultiplexer_PDStream<InstrumentMessage> {

        private final SelectionOptions options;

        CursorMultiplexer(SelectionOptions o) {
            super (!o.reversed, o.realTimeNotification, o.ordered ?
                    new MessageComparator<InstrumentMessage>() : new TimeComparator<InstrumentMessage>());
            this.options = o;
        }

        @Override
        public InstrumentMessage    createRealTimeMessage() {
            long currentTime = getCurrentTime();
            long time = currentTime != Long.MAX_VALUE ? currentTime : Long.MIN_VALUE;
            return TickStreamImpl.createRealTimeStartMessage(options.raw, time);
        }

        @Override
        public boolean isRealTimeMessage(InstrumentMessage message) {
            if (options.raw)
                return ((RawMessage)message).type.getGuid().equals(RealTimeStartMessage.DESCRIPTOR_GUID);

            return message instanceof RealTimeStartMessage;
        }

        @Override
        protected boolean isEmpty() {
            boolean resultFromParent = super.isEmpty();
            if (resultFromParent || !options.versionTracking || options.isLive()) {
                return resultFromParent;
            }
            return hasOnlyStreamVersionsReaders();
        }

        private boolean hasOnlyStreamVersionsReaders() {
            // TODO: remove this hack
            // ignore StreamVersionsReaders - they will block historical cursor next
            for (PrioritizedSource<InstrumentMessage> source : emptySources) {
                if (!(source.getSrc() instanceof StreamVersionsReader))
                    return false;
            }

            return true;
        }
    }

    /**
     * This method body must be exact copy of {@link TickCursorImpl#next()} method (see docs for class).
     * Failing to do so will result in incorrect application behavior.
     * Never edit parts of this method! Always edit {@link TickCursorImpl#next()} and copy-paste it's content into this
     * method.
     *
     * TODO: Implement automatic body copy from {@link TickCursorImpl#next()} to {@link TickCursorImpl_PDStream#next()}.
     */
    @Override
    public boolean              next () {
        boolean                 ret;
        RuntimeException        x = null;
        Runnable                lnr;

        synchronized (mx) {
            assert delayedListenerToCall == null : dlnrDiag ();

            for (;;) {
                boolean         hasNext;

                try {
                    hasNext = mx.syncNext ();
                } catch (RuntimeException xx) {
                    x = xx;
                    ret = false;    // make compiler happy
                    break;
                }

                if (!hasNext) {
                    ret = false;
                    break;
                }

                currentMessage = mx.syncGetMessage ();

                // current message is indicator of real-time mode
                if (realTimeNotifications && mx.isRealTimeStarted()) {
                    currentType = Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR;
                    ret = inRealtime = true;
                    break;
                }

                if (!isSubscribedToAllEntities && !isSubscribed(currentMessage)) {
                    if (DebugFlags.DEBUG_MSG_DISCARD) {
                        DebugFlags.discard (
                                "TB DEBUG: Discarding message " +
                                        currentMessage + " because we are not subscribed to its entity"
                        );
                    }

                    assertIsOpen();

                    continue;
                }

                final TypedMessageSource  source =
                        (TypedMessageSource) mx.syncGetCurrentSource ();

                // Instead of getting currentStream we will store currentSource so we can get currentStream lazily
                currentStream = null; //(((TickStreamRelated) source).getStream ());
//                // Store source to make it possible to get "currentStream" later
//                currentSource = source;

                if (currentMessage.getClass () == RawMessage.class)
                    currentType = ((RawMessage) currentMessage).type;
                else
                    currentType = source.getCurrentType ();

                if (subscribedTypeNames != null &&
                        !subscribedTypeNames.contains (currentType.getName ()))
                {
                    if (DebugFlags.DEBUG_MSG_DISCARD) {
                        DebugFlags.discard (
                                "TB DEBUG: Discarding message " +
                                        currentMessage + " because we are not subscribed to its type"
                        );
                    }

                    assertIsOpen();

                    continue;
                }

                stats.register (currentMessage);

                ret = true;
                break;
            }
            //
            //  Surprisingly, even mx.next () can call the av lnr (on truncation)
            //
            lnr = lnrTriggered ();
        }

        if (lnr != null)
            lnr.run ();

        if (x != null)
            throw x;

        return (ret);
    }


    public void                 addStream (TickStream ... tickStreams) {
        for (TickStream stream : tickStreams) {
            if (!(stream instanceof PDStream)) {
                throw new IllegalArgumentException(String.format("Can't add new stream type \"%s\" to fixed type cursor (\"%s\").", stream.getClass(), PDStream.class));
            }
        }
        super.addStream(tickStreams);
    }
}
