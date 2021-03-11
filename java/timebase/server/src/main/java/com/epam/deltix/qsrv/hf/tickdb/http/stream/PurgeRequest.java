package com.epam.deltix.qsrv.hf.tickdb.http.stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "purgeStream")
public class PurgeRequest extends StreamRequest {

    @XmlElement()
    public long             time;
}
