package com.epam.deltix.snmp.pub;

import java.lang.annotation.*;

/**
 *  Object description.
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target ( { ElementType.METHOD, ElementType.TYPE } )
public @interface Description {
    public String value ();
}
