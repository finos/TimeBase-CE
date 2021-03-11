package com.epam.deltix.qsrv.hf.tickdb.http.download;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "cursorResponse")
public class CursorResponse {

    @XmlElement()
    public long         serial;
}
