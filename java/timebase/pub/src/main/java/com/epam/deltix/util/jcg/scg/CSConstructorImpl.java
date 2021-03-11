package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.JExpr;

import java.io.IOException;

class CSConstructorImpl extends ConstructorImpl {
    private JCompStmtElem delegate;

    CSConstructorImpl (CSClassImpl container, int modifiers) {
        super (container, modifiers, null);
    }

    @Override
    public void                     callParent(final JExpr... args) {
        delegate = new JStatementImplBase(context) {
            @Override
            public void printElement(SourceCodePrinter out) throws IOException {
                out.print(": base (");
                CSharpSrcGenContext.px(out, args);
                out.print(")");
            }
        };
    }

    @Override
    public void                     call(final JExpr... args) {
        delegate = new JStatementImplBase(context) {
            @Override
            public void printElement(SourceCodePrinter out) throws IOException {
                out.print(": this (");
                CSharpSrcGenContext.px(out, args);
                out.print(")");
            }
        };
    }

    @Override
    protected void printExceptions(SourceCodePrinter out) throws IOException {
        // cs does not allow exceptions declarations

        // also print calls of delegating constructor respecting cs notation

        if (delegate != null)
            delegate.printElement(out);
    }

    @Override
    public void printHead(SourceCodePrinter out) throws IOException {
        super.printHead(out);
    }
}
