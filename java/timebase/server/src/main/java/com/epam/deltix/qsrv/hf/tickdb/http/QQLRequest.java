package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement(name = "selectQQL")
public class QQLRequest extends SelectRequest {

    @XmlElement()
    public String       qql;

    @XmlElementWrapper
    @XmlElement(name = "item")
    public QQLParameter[]      parameters;
}
