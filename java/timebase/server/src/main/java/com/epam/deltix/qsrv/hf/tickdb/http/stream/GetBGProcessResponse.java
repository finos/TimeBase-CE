package com.epam.deltix.qsrv.hf.tickdb.http.stream;


import com.epam.deltix.qsrv.hf.tickdb.pub.BackgroundProcessInfo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "BgProcess")
public class GetBGProcessResponse {

    public GetBGProcessResponse() {
    }

    public GetBGProcessResponse(BackgroundProcessInfo info) {
        this.info = info;
    }

    @XmlElement()
    public BackgroundProcessInfo info;

}
