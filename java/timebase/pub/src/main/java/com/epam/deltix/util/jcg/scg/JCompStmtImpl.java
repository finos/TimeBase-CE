package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;
import java.io.IOException;
import java.util.*;
import static com.epam.deltix.util.jcg.scg.JavaSrcGenContext.*;

/**
 *
 */
public class JCompStmtImpl 
    extends JStatementImplBase 
    implements Printable, JCompoundStatement 
{
    private final List <JCompStmtElem>      statements = new ArrayList <> ();

    public JCompStmtImpl (JContextImpl context) {
        super (context);
    }
    
    protected final void    add (JCompStmtElem e) {
        statements.add (e);
    }

    @Override
    public void             addComment (final String text) {
        statements.add (
            new JCompStmtElem () {
                @Override
                public void printElement (SourceCodePrinter out) throws IOException {
                    for (String s : text.split ("\\n")) {
                        out.newLine ();
                        out.print ("// ", s);
                    }
                }
            }
        );
    }

    @Override
    public void printElement(SourceCodePrinter out) throws IOException {
        print(out);
    }

    @Override
    public void     print (SourceCodePrinter out) throws IOException {
        out.print ("{");
        out.indent (+1);

        for (JCompStmtElem s : statements) {
            out.newLine ();
            s.printElement (out);
        }

        out.indent (-1);
        out.newLine ();
        out.print ("}");
        out.newLine ();
    }

    @Override
    public void     add (JStatement stmt) {
        add ((JCompStmtElem) stmt);
    }

    @Override
    public void addFront(JStatement stmt) {
        statements.add (0, (JCompStmtElem)stmt);
    }

    @Override
    public void     add (JExpr e) {
        add (etos ((JExprImplBase) e));
    }

    @Override
    public JLocalVariable  addVar (int modifiers, Class <?> type, String name) {
        return (addVar (modifiers, type, name, null));
    }

    @Override
    public JLocalVariable  addVar (int modifiers, Class <?> type, String name, JExpr initValue) {
        return (addVar (modifiers, context.classToType (type), name, initValue));
    }

    @Override
    public JLocalVariable  addVar (int modifiers, JType type, String name) {
        return (addVar (modifiers, type, name, null));
    }

    @Override
    public JLocalVariable  addVar (int modifiers, JType type, String name, JExpr initValue) {
        VarDeclImpl vdecl = 
            new VarDeclImpl (
                context, 
                context.refineModifiersForLocalVarDecl (modifiers), 
                cn (type), 
                name
            );

        if (initValue != null)
            vdecl.setInitValue (initValue);

        statements.add (vdecl);

        return (vdecl);
    }
}
