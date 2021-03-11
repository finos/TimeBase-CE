package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "createStreamRequest")
public class CreateStreamRequest extends XmlRequest {

    @XmlElement()
    public String                       key;

    @XmlElement(name = "options")
    public StreamDef                    options;
}
