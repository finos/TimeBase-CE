package com.epam.deltix.util.ldap.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */
public class Attribute {

    public Attribute() { } // JAXB

    public Attribute(String name, String field, String property) {
        this.name = name;
        this.field = field;
        this.property = property;
    }

    @XmlAttribute(name = "name")
    public String name;

    @XmlAttribute(name = "field")
    public String field;

    @XmlAttribute(name = "property")
    public String property;
}
