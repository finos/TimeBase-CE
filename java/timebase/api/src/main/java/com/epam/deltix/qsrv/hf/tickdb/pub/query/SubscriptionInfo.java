package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.timebase.messages.IdentityKey;
import org.agrona.collections.ObjectHashSet;

/**
 * Provides subscription information - subscribed entities and types.
 */
public interface SubscriptionInfo {
    /**
     *  Get the specified entities of the current subscription.
     *  @return not-nullable array of subscribed entities.
     */
    IdentityKey[]        getSubscribedEntities();

    /**
     *  Indicates that all entities subscribed (no filtering).
     *  @return true if all entities subscribed.
     */
    boolean                     isAllEntitiesSubscribed();

    /**
     *  Get the specified type names of the current subscription.
     *  @return not-nullable array of subscribed type names.
     */
    String[]                    getSubscribedTypes();

    /**
     *  Indicates that all types subscribed (no filtering).
     *  @return true if all types subscribed.
     */
    boolean                     isAllTypesSubscribed();

    /**
     *  Indicates true if types subscription is not empty.
     *  @return true if at least one type was subscribed.
     */
    boolean                     hasSubscribedTypes();

    default CharSequence[]      getSubscribedSymbols() {
        ObjectHashSet<CharSequence> set = new ObjectHashSet<>();
        for (IdentityKey id : getSubscribedEntities()) {
            set.add(id.getSymbol());
        }
        return set.toArray(new CharSequence[0]);
    }

    /**
     *  Indicates that all symbols subscribed (no filtering).
     *  @return true if all symbols subscribed.
     */
    default boolean             isAllSymbolsSubscribed() {
        return isAllEntitiesSubscribed();
    }


}
