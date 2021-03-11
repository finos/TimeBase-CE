package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import java.lang.annotation.*;

/**
 *  Information about a plug-in function.
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target ({ ElementType.TYPE })
public @interface FunctionInfo {   
    /**
     *  Id in QQL.
     */
    public String       id ();
    
    /**
     *  Return type (add "?" if nullable). Example: <code>INTEGER?</code>
     */
    public String       returns ();
    
    /**
     *  Argument types (add "?" if nullable). Example: <code>INTEGER?</code>
     */
    public String []    args ();
}
