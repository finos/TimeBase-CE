package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import java.util.*;

/**
 *  Common utility for filtering types. Requires external synchronization.
 */
public class HierarchicalTypeFilter implements TypeSubscriptionController {
    private Set <String>        allowedNames = null;
    private BitSet              allowedTypes = null;

    public boolean      accept (StreamMessageSource info) {
        throw new RuntimeException ();
    }

    public void         addTypes (String... names) {
        if (allowedNames == null)
            allowedNames = new HashSet <String> ();

        for (String s : names) {
            allowedNames.add (s);
        }
    }

    @Override
    public void setTypes(String... names) {
        if (allowedNames == null)
            allowedNames = new HashSet <String> ();
        else
            allowedNames.clear();

        allowedNames.addAll(Arrays.asList(names));
    }

    public void         removeTypes (String... names) {
        if (allowedNames == null)
            allowedNames = new HashSet <String> ();
        else {
            for (String s : names) {
                allowedNames.remove (s);
            }
        }
    }

    public void         subscribeToAllTypes () {
        allowedNames = null;
    }
}
