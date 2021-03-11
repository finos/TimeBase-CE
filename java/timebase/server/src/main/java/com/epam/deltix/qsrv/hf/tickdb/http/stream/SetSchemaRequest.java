package com.epam.deltix.qsrv.hf.tickdb.http.stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "setSchema")
public class SetSchemaRequest extends StreamRequest {

    @XmlElement()
    public boolean  polymorphic;

    @XmlElement()
    public String   schema;

    @XmlElement()
    public String   type = "XML";
}
