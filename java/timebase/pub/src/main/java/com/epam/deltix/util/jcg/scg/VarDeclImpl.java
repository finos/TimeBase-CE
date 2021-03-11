package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.JLocalVariable;
import java.io.IOException;

/**
 *
 */
final class VarDeclImpl
    extends InitVariableImpl
    implements JCompStmtElem, JLocalVariable
{
    public VarDeclImpl (JContextImpl context, int modifiers, String type, String name) {
        super (context, modifiers, type, name);
    }

    @Override
    public void         printElement (SourceCodePrinter out) throws IOException {
        printDeclaration (out);
    }
}
