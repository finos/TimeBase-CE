package com.epam.deltix.qsrv.hf.tickdb.pub.query;

/**
 *  Controls subscription for specific message types. Methods have no return
 *  value, because they can act asynchronously. The implementor of this
 *  interface is not required to remember the subscription details; therefore,
 *  there are no methods provided to query the current state (such as
 *  "hasType", etc.). The user of this interface can independently track
 *  current subscription, if so desired.
 */
public interface TypeSubscriptionController {
    /**
     *  Subscribe to all available types (no filtering).
     */
    public void                     subscribeToAllTypes ();

    /**
     *  Subscribe to specified types.
     */
    public void                     setTypes(String ... names);

    /**
     *  Add the specified type names to subscription.
     */
    public void                     addTypes (String ... names);

    /**
     *  Remove the specified types from subscription.
     */
    public void                     removeTypes (String ... names);
}
