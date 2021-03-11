package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.tickdb.http.XmlRequest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement(name = "loadStreams")
public class LoadStreamsRequest extends XmlRequest {

//    @XmlElementWrapper
//    @XmlElement(name = "item")
//    String[]    streams;
}
