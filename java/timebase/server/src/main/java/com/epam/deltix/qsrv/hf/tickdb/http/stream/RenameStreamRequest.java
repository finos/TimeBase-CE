package com.epam.deltix.qsrv.hf.tickdb.http.stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "renameStream")
public class RenameStreamRequest extends StreamRequest {

    @XmlElement()
    public String key;
}
