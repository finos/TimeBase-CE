package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 */
@XmlType()
public class TimeRange {
    @XmlElement()
    public long from;

    @XmlElement()
    public long to;

    // JAXB
    TimeRange() {
    }

    public TimeRange(long from, long to) {
        this.from = from;
        this.to = to;
    }
}
