package com.epam.deltix.snmp.pub;

import java.lang.annotation.*;

/**
 *  SMI type name
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target ( ElementType.METHOD )
public @interface SMITypeName {
    public String   value ();
}
