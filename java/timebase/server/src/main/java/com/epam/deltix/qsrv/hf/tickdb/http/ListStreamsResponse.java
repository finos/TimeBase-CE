package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement(name = "listStreamsResponse")
public class ListStreamsResponse {

    @XmlElementWrapper
    @XmlElement(name = "item")
    public String[] streams;
}
