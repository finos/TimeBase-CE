package com.epam.deltix.util.ldap.config;

import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 */
public class Credentials {

    @XmlElement(name = "username")
    public String name;

    @XmlElement(name = "key")
    public String key;

    public Credentials() { }// JAXB

    public Credentials(String name, String password) {
        this.name = name;
        this.key = IOUtil.concat(password, name);
    }

    public String         get() {
        return IOUtil.split(key, name);
    }
}
