package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement(name = "loadStreamsResponse")
public class LoadStreamsResponse {

    @XmlElementWrapper
    @XmlElement(name = "item")
    public String[]    streams;

    @XmlElementWrapper
    @XmlElement(name = "item")
    public StreamDef[] options;
}
