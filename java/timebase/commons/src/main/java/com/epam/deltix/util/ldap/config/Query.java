package com.epam.deltix.util.ldap.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */

public class Query {

    public Query() { } // JAXB

    public Query(String node, String filter) {
        this.node = node;
        this.filter = filter;
    }

    @XmlAttribute(name = "node")
    public String node;

    @XmlAttribute(name = "filter")
    public String filter;
}