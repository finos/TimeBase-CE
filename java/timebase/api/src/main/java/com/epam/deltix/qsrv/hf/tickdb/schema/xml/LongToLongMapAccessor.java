package com.epam.deltix.qsrv.hf.tickdb.schema.xml;

import com.epam.deltix.util.collections.generated.LongToLongHashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class LongToLongMapAccessor {

    @XmlElement
    @XmlJavaTypeAdapter(LongToLongMapAdapter.class)
    public LongToLongHashMap map;

    protected LongToLongMapAccessor() {
    }

    public LongToLongMapAccessor(LongToLongHashMap map) {
        this.map = map;
    }
}
