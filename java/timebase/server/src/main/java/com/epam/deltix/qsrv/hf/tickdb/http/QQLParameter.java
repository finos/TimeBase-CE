package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;

import javax.xml.bind.annotation.XmlElement;

public class QQLParameter {

    @XmlElement()
    public String name;

    @XmlElement()
    public String type; // StandardTypes

    @XmlElement()
    public String value;

    public QQLParameter() { // JAXB
    }

    public QQLParameter(String name, DataType dataType, Object value) {
        this.name = name;
        this.type = StandardTypes.toSimpleName(dataType);
        this.value = String.valueOf(value);
    }

    public QQLParameter(String name, String dataType, Object value) {
        this.name = name;
        this.type = dataType;
        this.value = String.valueOf(value);
    }

    public Parameter    toParameter() {
        Parameter p = new Parameter(name, StandardTypes.forName(type));

        if (value != null)
            p.value.writeString(value);
        else
            p.value.writeNull();
        return p;
    }
}
