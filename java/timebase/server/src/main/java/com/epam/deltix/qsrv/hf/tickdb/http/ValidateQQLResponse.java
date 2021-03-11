package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "validateQQLResponse")
public class ValidateQQLResponse {

    public ValidateQQLResponse() {
    }

    public ValidateQQLResponse(QQLState state) {
        this.result = state;
    }

    @XmlElement()
    public QQLState result;
}
