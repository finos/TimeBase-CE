package com.epam.deltix.snmp.pub;

import java.lang.annotation.*;

/**
 *  OCTET STRING size
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target ( { ElementType.METHOD } )
public @interface Size {
    public int value ();
}
