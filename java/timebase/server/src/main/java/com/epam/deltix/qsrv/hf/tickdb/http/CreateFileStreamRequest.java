package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "createFileStreamRequest")
public class CreateFileStreamRequest extends XmlRequest {

    @XmlElement()
    public String                       key;

    @XmlElement
    public String                       dataFile;
}
