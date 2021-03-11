package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;
import java.io.IOException;

/**
 *
 */
final class JConstructorImpl extends ConstructorImpl {
    JConstructorImpl (
        ClassImpl                   parent,
        int                         modifiers
    )
    {
        super (parent, modifiers, null);
    }

    @Override
    public void                     callParent(final JExpr... args) {
        body.add (
            new JStatementImplBase (context) {
                @Override
                public void     printElement (SourceCodePrinter out) throws IOException {
                    out.print ("super (");
                    JavaSrcGenContext.px (out, args);
                    out.print (");");
                }
            }
        );
    }

    @Override
    public void                     call(final JExpr... args) {
        body.add (
                new JStatementImplBase (context) {
                    @Override
                    public void     printElement (SourceCodePrinter out) throws IOException {
                        out.print ("this (");
                        JavaSrcGenContext.px (out, args);
                        out.print (");");
                    }
                }
        );
    }
}
