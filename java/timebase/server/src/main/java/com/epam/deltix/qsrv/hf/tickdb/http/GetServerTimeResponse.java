package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "getServerTimeResponse")
public class GetServerTimeResponse {

    // JAXB
    public GetServerTimeResponse() {  }

    public GetServerTimeResponse(long time) {
        this.time = time;
    }

    @XmlElement()
    public long time;
}
