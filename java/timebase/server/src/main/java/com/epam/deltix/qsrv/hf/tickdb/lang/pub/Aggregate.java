package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import java.lang.annotation.*;

/**
 *  Tags a plug-in function as aggregate.
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target ({ ElementType.TYPE })
public @interface Aggregate {   
}
