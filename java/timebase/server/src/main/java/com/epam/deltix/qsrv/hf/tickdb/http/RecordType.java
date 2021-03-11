package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 */
@XmlType(name = "recordType")
public class RecordType {
    @XmlElement()
    public String name;

    @XmlElement(name = "column")
    public Column[] columns;
}
