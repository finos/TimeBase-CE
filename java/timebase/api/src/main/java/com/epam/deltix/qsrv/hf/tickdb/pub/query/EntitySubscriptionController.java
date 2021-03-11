package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 *  Controls subscription for a number of entities. Methods have no return
 *  value, because they can act asynchronously. The implementor of this
 *  interface is not required to remember the subscription details; therefore,
 *  there are no methods provided to query the current state (such as
 *  "hasEntity", etc.). The user of this interface can independently track
 *  current subscription, if so desired.
 */
public interface EntitySubscriptionController {
    /**
     *  Subscribe to all available entities.
     */
    void                     subscribeToAllEntities ();

    /**
     *  Switch to selective subscription mode (if necessary) and clear the list.
     */
    void                     clearAllEntities ();

    /**
     *  Add the specified entity to subscription. The type and symbol are copied
     *  from the incoming object, if necessary, so the argument can be re-used
     *  after the call.
     *
     *  Special note about options: The following fragment will subscribe to specific option contract "DAV   100417P00085000":
     *  <code>
     *  addEntity(new InstrumentKey(InstrumentType.OPTION, "DAV   100417P00085000"));
     *  </code>
     *  While the following will subscribe to option root (and you will get all instruments with this root):
     *  <code>
     *  addEntity(new InstrumentKey(InstrumentType.OPTION, "DAV   "));
     *  </code>
     *
     */
    void                     addEntity (IdentityKey id);

    /**
     *  Bulk add the specified entities to subscription. The type and symbol are copied
     *  from the incoming objects, if necessary, so the arguments can be re-used
     *  after the call.
     */
    void                     addEntities(
            IdentityKey[] ids,
            int offset,
            int length
    );

    default void             addEntities(IdentityKey[] ids) {
        addEntities(ids, 0, ids.length);
    }

    /**
     *  Remove the specified entity from subscription. The type and symbol are copied
     *  from the incoming object, if necessary, so the argument can be re-used
     *  after the call.
     */
    void                     removeEntity (IdentityKey id);

    /**
     *  Remove the specified entities from subscription. The type and symbol are copied
     *  from the incoming objects, if necessary, so the arguments can be re-used
     *  after the call.
     */
    void                     removeEntities (
        IdentityKey []           ids,
        int                             offset,
        int                             length
    );

    default void             removeEntities (
            IdentityKey []           ids
    ) {
        removeEntities(ids, 0, ids.length);
    }
}
