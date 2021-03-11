package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 */
@XmlType(name = "column")
public class Column {
    @XmlElement()
    String name;

    @XmlElement()
    int size = 0;

    // JAXB
    protected Column() {
    }

    public Column(String name) {
        this.name = name;
    }

    public Column(String name, int size) {
        this.name = name;
        this.size = size;
    }
}
