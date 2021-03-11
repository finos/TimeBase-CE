package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "validateQQL")
public class ValidateQQLRequest extends XmlRequest {

    @XmlElement()
    public String qql;
}
