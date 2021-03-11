package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 */
public abstract class DownloadRequest extends XmlRequest {

    @XmlElement()
    public long from = Long.MIN_VALUE;

    @XmlElement()
    public long to = Long.MAX_VALUE;

    @XmlElement
    public boolean isBigEndian = false;

    @XmlElement()
    public boolean useCompression = false;

    @XmlElement()
    public boolean minLatency = false;

    @XmlElement(name = "token")
    public String token;
}
