package com.epam.deltix.snmp.pub;

import java.lang.annotation.*;

/**
 *  User-defined id
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target ( { ElementType.METHOD } )
public @interface SizeRange {
    public int min ();
    public int max ();
}
