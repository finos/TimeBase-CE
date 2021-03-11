package com.epam.deltix.qsrv.hf.tickdb.http.download;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "changeStreams")
public class StreamsRequest extends CursorRequest {

    @XmlElement()
    public ChangeAction         mode;

    @XmlElementWrapper
    @XmlElement(name = "item")
    public String[]             streams;
}
