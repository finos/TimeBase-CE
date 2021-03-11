package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;
import java.io.*;
import java.lang.reflect.*;

/**
 *
 */
class CSMethodImpl extends MethodImpl {
    private boolean         isOverride = false;
    
    CSMethodImpl (ClassImpl container, int modifiers, String type, String name) {
        super (container, modifiers, type, name);
    }

    @Override
    public void             addAnnotation (JAnnotation annotation) {
        if (annotation == CSharpSrcGenContext.OVERRIDE_PSEUDO_ANNOTATION)
            isOverride = true;
        else
            super.addAnnotation (annotation);
    }   

    @Override
    void                    printModifiers (SourceCodePrinter out) 
        throws IOException 
    {
        if (isOverride)
            out.print ("override ");
        
        context.printModifiers (modifiers () & ~Modifier.FINAL, out);            
    }        
}
