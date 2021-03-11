package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Collection;
import java.util.HashSet;

/**
 * Adaptor to JAXB binding list of InstrumentIdentities
 *
 * User: TurskiyS
 * Date: 7/20/12
 */
public  class IdentityKeyListAdapter
        extends
        XmlAdapter<IdentityKeyAdapter[], IdentityKey[]> {
    @Override
    public IdentityKeyAdapter[] marshal (final IdentityKey[] v) throws Exception {
        if (v == null) {
            return null;
        }

        final IdentityKeyAdapter[] res = new IdentityKeyAdapter[v.length];
        int idx = 0;

        for (final IdentityKey id : v) {
            res[idx++] = new IdentityKeyAdapter (id);
        }

        return res;
    }

    @Override
    public IdentityKey[] unmarshal (final IdentityKeyAdapter[] v) throws Exception {
        if (v == null) {
            return null;
        }

        final Collection<IdentityKey> res = new HashSet<IdentityKey>();

        for (final IdentityKeyAdapter iia : v) {
            res.add (new ConstantIdentityKey(iia.symbol));
        }
        IdentityKey[] entities = new IdentityKey[res.size()];
        res.toArray(entities);
        if (entities.length == 0)
            return null;

        return entities;
    }
}
