package com.epam.deltix.qsrv.hf.security.rules.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.epam.deltix.util.security.AccessControlRule.ResourceFormat;
import com.epam.deltix.util.security.AccessControlRule.ResourceType;

public class Resource {
    public Resource() {};

    public Resource(String value) {
        this.value = value;
    }

    @XmlAttribute(name = "type")
    public ResourceType type = ResourceType.Principal;

    @XmlAttribute(name = "format")
    public ResourceFormat format = ResourceFormat.Text;

    @XmlValue
    public String value;
}
