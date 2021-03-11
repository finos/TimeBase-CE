package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.timebase.messages.IdentityKey;

import java.util.Collection;

public interface EntitySubscriptionChangeListener {

    void entitiesAdded(Collection<IdentityKey> entities);

    void entitiesRemoved(Collection<IdentityKey> entities);

    void allEntitiesAdded();

    void allEntitiesRemoved();
}