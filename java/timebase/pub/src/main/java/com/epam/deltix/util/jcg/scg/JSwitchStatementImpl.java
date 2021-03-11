package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JSwitchStatement;
import java.io.IOException;

/**
 *
 */
class JSwitchStatementImpl
    extends JCompStmtImpl
    implements JSwitchStatement
{
    private final String    label;
    private final JExpr     discriminator;

    JSwitchStatementImpl (String label, JExprImplBase discriminator) {
        super (discriminator.context);
        
        this.label = label;
        this.discriminator = discriminator;
    }

    @Override
    public void     print (SourceCodePrinter out) throws IOException {
        out.print (label, ": switch (", discriminator, ") ");
        super.print (out);
    }

    @Override
    public void             addBreak () {
        add (context.mkbreak (label));
    }

    @Override
    public void             addCaseLabel (final JExpr e) {
        addCaseLabel (e, null);
    }

    @Override
    public void             addCaseLabel (final JExpr e, final String comment) {
        add (
            new JCompStmtElem () {
                @Override
                public void printElement (SourceCodePrinter out) throws IOException {
                    out.indent (-1);
                    out.print ("case ", e, ":");
                    if (comment != null)
                        out.print (" // " + comment);                    
                    out.indent (1);
                }
            }
        );
    }

    @Override
    public void             addDefaultLabel () {
        add (
            new JCompStmtElem () {
                @Override
                public void printElement (SourceCodePrinter out) throws IOException {
                    out.indent (-1);
                    out.print ("default:");
                    out.indent (1);
                }
            }
        );
    }

}
