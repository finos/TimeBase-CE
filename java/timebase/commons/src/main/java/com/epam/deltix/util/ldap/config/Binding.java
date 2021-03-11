package com.epam.deltix.util.ldap.config;

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;

/**
 *
 */
public class Binding {

    public Binding() { } // JAXB

    @XmlElement(name = "objectClass")
    public ArrayList<String> objectsClasses = new ArrayList<String>();

    @XmlElement(name = "attribute")
    public ArrayList<Attribute> attributes = new ArrayList<Attribute>();
}
