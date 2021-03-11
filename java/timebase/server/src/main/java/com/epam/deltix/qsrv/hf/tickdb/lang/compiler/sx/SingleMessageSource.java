package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.msgsrcs.SingleMessageEmitter;
import java.util.Set;

/**
 *
 */
public class SingleMessageSource extends CompiledQuery {
    public static final QueryDataType       TYPE = 
        new QueryDataType (
            false,
            new ClassDataType (false, SingleMessageEmitter.VOID_TYPE)
        );
    
    public SingleMessageSource () {
        super (TYPE);
    }

    @Override
    public boolean              isForward () {
        return (true);
    }
    
    @Override
    protected void              print (StringBuilder out) {
        out.append ("<void source>");
    }        
    
    @Override
    public void                getAllTypes (Set <ClassDescriptor> out) {
        out.add (SingleMessageEmitter.VOID_TYPE);
    }
}
