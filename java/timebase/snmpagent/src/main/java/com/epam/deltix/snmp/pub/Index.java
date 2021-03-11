package com.epam.deltix.snmp.pub;

import java.lang.annotation.*;

/**
 *  Index attribute (parameterized by relative position)
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target ( ElementType.METHOD )
public @interface Index {
    public int value () default 0;
}
