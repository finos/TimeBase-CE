package com.epam.deltix.qsrv.hf.security.rules.xml;

import javax.xml.bind.annotation.XmlElement;

public class Rule {
    public Rule() {}

    @XmlElement(name = "principal", required = true)
    public String[] principals;

    @XmlElement (name = "permission", required = true)
    public String[] permissions;

    @XmlElement (name = "resource")
    public Resource[] resources;
}
