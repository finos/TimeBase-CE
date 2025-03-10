package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Collection;
import java.util.HashSet;

import com.epam.deltix.qsrv.hf.tickdb.http.InstrumentIdentityKeyListAdapter.InstrumentIdentityKeyAdapter;

/**
 *
 */
public class InstrumentIdentityKeyListAdapter extends
    XmlAdapter<InstrumentIdentityKeyAdapter[], InstrumentIdentityKey[]> {
    @Override
    public InstrumentIdentityKeyAdapter[] marshal(final InstrumentIdentityKey[] v) throws Exception {
        if (v == null) {
            return null;
        }

        final InstrumentIdentityKeyAdapter[] res = new InstrumentIdentityKeyAdapter[v.length];
        int idx = 0;

        for (final InstrumentIdentityKey id : v) {
            res[idx++] = new InstrumentIdentityKeyAdapter(id);
        }

        return res;
    }

    @Override
    public InstrumentIdentityKey[] unmarshal(final InstrumentIdentityKeyAdapter[] v) throws Exception {
        if (v == null)
            return null;

        final Collection<InstrumentIdentityKey> res = new HashSet<InstrumentIdentityKey>();

        for (final InstrumentIdentityKeyAdapter iia : v) {
            res.add(new InstrumentIdentityKey(iia.instrumentType, iia.symbol));
        }

        InstrumentIdentityKey[] entities = new InstrumentIdentityKey[res.size()];
        res.toArray(entities);

        return entities;
    }

    public static class InstrumentIdentityKeyAdapter {
        @XmlElement(namespace = "")
        public String instrumentType;

        @XmlElement(namespace = "")
        public String symbol;

        public InstrumentIdentityKeyAdapter() {
        }

        public InstrumentIdentityKeyAdapter(final InstrumentIdentityKey id) {
            super();
            instrumentType = id.getInstrumentType();
            symbol = id.getSymbol().toString();
        }
    }
}
