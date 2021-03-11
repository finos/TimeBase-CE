package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.timebase.messages.IdentityKey;

import javax.xml.bind.annotation.XmlElement;

/**
 * Adapter to JAXB binding IdentityKey
 *
 * User: TurskiyS
 * Date: 7/20/12
 */
public class IdentityKeyAdapter {
    @XmlElement
    public String         symbol;

    public IdentityKeyAdapter () {
    }

    public IdentityKeyAdapter (final IdentityKey id) {
        super ();
        symbol = id.getSymbol ().toString ();
    }
}
