package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import java.util.Collection;

public interface TypeSubscriptionChangeListener {

    void typesAdded(Collection<String> types);

    void typesRemoved(Collection<String> types);

    void allTypesAdded();

    void allTypesRemoved();
}