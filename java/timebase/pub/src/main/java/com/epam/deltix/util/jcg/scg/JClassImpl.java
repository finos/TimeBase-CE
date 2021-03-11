package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;

import java.io.IOException;

import static com.epam.deltix.util.jcg.scg.JContextImpl.px;

/**
 *
 */
final class JClassImpl extends ClassImpl {
    JClassImpl (ClassImpl outer, int modifiers, String simpleName, String parentName) {
        super (outer, modifiers, simpleName, parentName);
    }

    JClassImpl (JContextImpl context, int modifiers, String packageName, String simpleName, String parentName) {
        super (context, modifiers, packageName, simpleName, parentName);
    }

    @Override
    public void         printDeclaration (SourceCodePrinter out)
        throws IOException
    {
        out.newLine ();

        JClass save = out.currentClass;
        out.currentClass = this;

        printAnnotations (out);
        
        out.printModifiers(modifiers());
        out.print ("class ", name ());

        if (parentName != null) {
            out.print (" extends ");
            out.printRefClassName (parentName);
        }
        
        int             n = interfaceNames.size ();

        if (n > 0) {
            out.print (" implements ", interfaceNames.get (0));

            for (int ii = 1; ii < n; ii++)
                out.print (", ", interfaceNames.get (ii));
        }

        out.print (" {");
        out.indent (1);

        for (JMemberIntf m : members)
            m.printDeclaration (out);

        out.indent (-1);
        out.newLine ();
        out.println ("}");

        out.currentClass = save;
    }

    @Override
    public JExpr callSuperMethod (final String name, final JExpr ... args) {
        return (
            new JExprImplBase (context) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print ("super.", name, " (");
                    px (out, args);
                    out.print (")");
                }
            }
        );
    }
    
    @Override
    ConstructorImpl         newConstructor (int modifiers) {
        return new JConstructorImpl (this, modifiers);
    }
    
    @Override
    ClassImpl               innerClassImpl (int modifiers, String simpleName, String parentName) {
        return new JClassImpl (this, modifiers, simpleName, parentName);
    }   

    @Override
    JMethodImpl             createMethod (int modifiers, String typeName, String name) {
        return (new JMethodImpl (this, modifiers, typeName, name));
    }        
}
