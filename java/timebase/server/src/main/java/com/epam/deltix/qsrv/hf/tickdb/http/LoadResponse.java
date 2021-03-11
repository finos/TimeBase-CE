package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement(name = "loadResponse")
public class LoadResponse {
    @XmlElement()
    public boolean wasError;

    @XmlElement()
    public String responseMessage;

    @XmlElement()
    public String details;
}
