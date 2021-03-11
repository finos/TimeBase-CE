package com.epam.deltix.qsrv.hf.tickdb.http.download;

import com.epam.deltix.qsrv.hf.tickdb.http.XmlRequest;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;

import javax.xml.bind.annotation.XmlElement;

public abstract class CursorRequest extends XmlRequest {

    @XmlElement()
    public long                 id;

    @XmlElement()
    public long                 time = TimeConstants.TIMESTAMP_UNKNOWN;

    public CursorRequest() {
    }

    public CursorRequest(long id) {
        this.id = id;
    }
}
