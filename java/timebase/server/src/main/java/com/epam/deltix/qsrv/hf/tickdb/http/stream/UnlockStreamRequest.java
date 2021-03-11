package com.epam.deltix.qsrv.hf.tickdb.http.stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "unlockStream")
public class UnlockStreamRequest extends StreamRequest {

    public UnlockStreamRequest() {}

    public UnlockStreamRequest(String id, boolean write) {
        this.id = id;
        this.write = write;
    }

    @XmlElement(name = "id")
    public String   id;

    @XmlElement(name = "write")
    public boolean  write;
}
