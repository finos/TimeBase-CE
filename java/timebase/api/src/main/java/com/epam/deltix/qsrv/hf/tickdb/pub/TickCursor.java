package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.qsrv.hf.pub.*;

/**
 *  A cursor (also known as iterator, or result set) for reading data from a
 *  stream. This class provides implements {@link deltix.qsrv.hf.tickdb.pub.query.SubscriptionController}
 *  for dynamically reconfiguring the feed, as well as method {@link #reset} for
 *  essentially re-opening the cursor on a completely different timestamp.
 *
 *  This class extends [sequential] FilteredMessageSource with random access
 *  to underlying source using {@link #reset(long)} method.
 */
public interface TickCursor extends InstrumentMessageSource {
    /**
     *  Re-open readers for given entities from specified time. Calling this
     *  method may be substantially faster than opening an entire new cursor,
     *  especially  in remote mode.
     *
     * @param time          The time to use, or {@link TimeConstants#USE_CURRENT_TIME}
     *                      to use server's system time, or
     *                      {@link TimeConstants#USE_CURSOR_TIME} to use cursor's
     *                      last read time.
     * @param instruments   List of instrument to reset. Should have at least one
     *                      entity to proceed.
     */
//    public void                 reset (long time, IdentityKey ... instruments);

//    /**
//     *  This method affects subsequent "add subscription" methods,
//     *  such as, for instance, addEntity (). New subscriptions start at
//     *  the specified time.
//     *
//     *  @param time     The time to use, or {@link TimeConstants#USE_CURRENT_TIME}
//     *                  to use server's system time, or
//     *                  {@link TimeConstants#USE_CURSOR_TIME} to use cursor's
//     *                  last read time.
//     */
//    public void                 setTimeForNewSubscriptions (long time);

//    /**
//     *  Check whether this cursor is closed, either by user, or by system.
//     */
//    public boolean              isClosed ();
//
//    /**
//     *  Returns the current message.
//     */
//    public InstrumentMessage    getMessage ();
}
