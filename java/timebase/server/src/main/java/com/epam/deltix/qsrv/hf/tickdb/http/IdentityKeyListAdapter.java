package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Collection;
import java.util.HashSet;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

/**
 *
 */
public class IdentityKeyListAdapter extends XmlAdapter<String[], IdentityKey[]> {
    @Override
    public String[] marshal(final IdentityKey[] v) throws Exception {
        if (v == null) {
            return null;
        }

        final String[] res = new String[v.length];
        int idx = 0;

        for (final IdentityKey id : v) {
            res[idx++] = id.getSymbol().toString();
        }

        return res;
    }

    @Override
    public IdentityKey[] unmarshal(final String[] v) throws Exception {
        if (v == null)
            return null;

        final Collection<IdentityKey> res = new HashSet<IdentityKey>();

        for (final String iia : v) {
            res.add(new ConstantIdentityKey(iia));
        }

        IdentityKey[] entities = new IdentityKey[res.size()];
        res.toArray(entities);

        return entities;
    }
}
