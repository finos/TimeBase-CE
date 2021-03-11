package com.epam.deltix.qsrv.hf.tickdb.schema.xml;

import com.epam.deltix.util.collections.generated.LongEnumeration;
import com.epam.deltix.util.collections.generated.LongToLongHashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;

public class LongToLongMapAdapter extends XmlAdapter<LongToLongMapAdapter.LongMapList, LongToLongHashMap> {

    @Override
    public LongToLongHashMap unmarshal(LongMapList v) throws Exception {
        LongToLongHashMap map = new LongToLongHashMap(v.entries.size());
        for (LongEntry entry : v.entries) {
            map.put(entry.key, entry.value);
        }
        return map;
    }

    @Override
    public LongMapList marshal(LongToLongHashMap v) {
        LongEnumeration enumeration = v.keys();
        ArrayList<LongEntry> list = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            long key = enumeration.nextLongElement();
            list.add(new LongEntry(key, v.get(key,-1)));
        }
        return new LongMapList(list);
    }

    public static class LongMapList {
        @XmlElements(@XmlElement(type=LongEntry.class))
        public ArrayList<LongEntry> entries;

        protected LongMapList() { } // for jaxb

        public LongMapList(ArrayList<LongEntry> entries) {
            this.entries = entries;
        }
    }

    public static class LongEntry {
        @XmlElement
        public long key;

        @XmlElement
        public long value;

        protected LongEntry() { } // for jaxb

        public LongEntry(long key, long value) {
            this.key = key;
            this.value = value;
        }
    }
}
