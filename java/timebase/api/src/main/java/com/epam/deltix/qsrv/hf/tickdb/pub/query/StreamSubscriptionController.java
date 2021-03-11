package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;

/**
 *  Controls the set of original streams from which information is being selected.
 *  Methods have no return
 *  value, because they can act asynchronously. The implementor of this
 *  interface is not required to remember the subscription; therefore,
 *  there are no methods provided to query the current state (such as
 *  "hasStream", "getStreams", etc.). The user of this interface can
 *  independently track current subscription, if so desired.
 */
public interface StreamSubscriptionController {
    /**
     *  Add streams to subscription. Current time and filter is used to query
     *  data from new sources.
     *
     *  @param tickStreams  Streams to add.
     */
    public void                 addStream (TickStream ... tickStreams);

    /**
     *  Remove all streams from subscription.
     */
    public void                 removeAllStreams ();

    /**
     *  Remove streams from subscription.
     *
     *  @param tickStreams  Streams to remove.
     */
    public void                 removeStream (TickStream ... tickStreams);
}
