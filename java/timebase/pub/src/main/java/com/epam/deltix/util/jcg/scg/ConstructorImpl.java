package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.JConstructor;

import java.io.IOException;

abstract class ConstructorImpl extends CallableImpl
    implements JConstructor 
{
    protected ConstructorImpl (ClassImpl container, int modifiers, String name) {
        super (container, modifiers, name);
    }

    @Override
    void                            printHead (SourceCodePrinter out)
        throws IOException
    {
        out.print (containerClass ().name ());
    }
}
