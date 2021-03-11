package com.epam.deltix.qsrv.hf.tickdb.http.download;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "closeRequest")
public class CloseRequest extends CursorRequest {

    public CloseRequest() {
    }

    public CloseRequest(long id) {
        super(id);
    }
}
