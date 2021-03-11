package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.*;
import java.util.Set;

/**
 *
 */
public abstract class CompiledQuery extends CompiledExpression <QueryDataType> {
    protected CompiledQuery (QueryDataType type) {
        super (type);
    }

    public abstract boolean             isForward ();
    
    /**
     *  Get concrete types of messages that can be returned by this query.
     */
    public RecordClassDescriptor []     getConcreteOutputTypes () {
        return (((ClassDataType) type.getOutputType ()).getDescriptors ());
    }
    
    /**
     *  Get all types related to this query. In case of a stream, this 
     *  returns all configured metadata in the stream.
     */
    public abstract void                getAllTypes (Set <ClassDescriptor> out);        
}
