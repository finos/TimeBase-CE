package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler;
import java.util.ArrayList;

/**
 *
 */
public class OverloadedFunctionSet {
    public final String                             id;
    private final ArrayList <FunctionDescriptor>    descriptors =
        new ArrayList <FunctionDescriptor> ();
    private final ArrayList <DataType []>           argMap =
        new ArrayList <DataType []> ();

    public OverloadedFunctionSet (String id) {
        this.id = id;
    }        
    
    public void         add (FunctionDescriptor fd) {
        descriptors.add (fd);
        
        String []               args = fd.info.args ();
        int                     numArgs = args.length;
        
        while (argMap.size () <= numArgs)
            argMap.add (null);
        
        DataType []    union = argMap.get (numArgs);
        
        if (union == null) 
            argMap.set (numArgs, fd.signature.clone ());        
        else {
            for (int ii = 0; ii < numArgs; ii++) 
                union [ii] = QQLCompiler.unionEx (union [ii], fd.signature [ii]);
        }            
    }
    
    public DataType []      getSignature (int numArgs) {
        if (argMap.size () <= numArgs)
            return (null);
        
        return (argMap.get (numArgs));
    }
    
    public FunctionDescriptor   getDescriptor (DataType [] argTypes) {
        FunctionDescriptor          ret = null;
        
        for (FunctionDescriptor fd : descriptors) {
            if (fd.accept (argTypes)) {
                if (ret == null)
                    ret = fd;
                else
                    return (null);
            }                
        }
        
        return (ret);
    }
}
