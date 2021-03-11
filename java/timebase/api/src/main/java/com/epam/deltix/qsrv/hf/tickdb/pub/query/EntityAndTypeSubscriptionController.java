package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 * A combination of EntitySubscriptionController and TypeSubscriptionController frequently used together in UHF.
 *
 */
public interface EntityAndTypeSubscriptionController 
    extends EntitySubscriptionController, TypeSubscriptionController 
{
    /**
     *  Add the specified entities and types to subscription. The type and symbol are copied
     *  from the incoming object, if necessary, so the argument can be re-used
     *  after the call.
     *
     * @param entities not-null array of instruments to subscribe.
     * @param types not-null array of type names to subscribe.
     */

    default void add(IdentityKey[] entities, String[] types) {
        addEntities(entities);
        addTypes(types);
    }

    /**
     *  Remove the specified entities and types from subscription. The type and symbol are copied
     *  from the incoming objects, if necessary, so the arguments can be re-used
     *  after the call.
     *
     *  @param entities not-null array of instruments to subscribe.
     *  @param types not-null array of type names to subscribe.
     */

    default void remove(IdentityKey[] entities, String[] types) {
        removeEntities(entities);
        removeTypes(types);
    }
}
