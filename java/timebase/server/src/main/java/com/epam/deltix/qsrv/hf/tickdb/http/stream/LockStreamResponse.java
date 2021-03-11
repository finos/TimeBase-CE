package com.epam.deltix.qsrv.hf.tickdb.http.stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "lockStreamsResponse")
public class LockStreamResponse {

    @XmlElement
    public String   id;

    @XmlElement
    public boolean  write;
}
