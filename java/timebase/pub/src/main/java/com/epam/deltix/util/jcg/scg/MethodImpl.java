package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.epam.deltix.util.jcg.scg.JavaSrcGenContext.*;

/**
 *
 */
 class MethodImpl
    extends CallableImpl
    implements JMethod
{
    private final String                  type;
    private final String                  name;
    private final Collection <JAnnotation> annotations = new ArrayList<> ();

    MethodImpl(
        ClassImpl                   container,
        int                         modifiers,
        String                      type,
        String                      name
    )
    {
        super (container, modifiers, name);
        this.type = type;
        this.name = name;
    }


    @Override
    public void                     printDeclaration (SourceCodePrinter out)
        throws IOException
    {
        if ( annotations.size () > 0) {
            out.newLine ();
            for (JAnnotation annotation : annotations) {
                out.print (annotation);
                out.newLine ();
            }
        }
        super.printDeclaration (out);
    }

    @Override
    void                            printHead (SourceCodePrinter out) throws IOException {
        context.printType (type, out);
        out.print (" ", name);
    }

    @Override
    public JExpr                    call (final JExpr obj, final JExpr... args) {
        return (
            new JExprImplBase (context) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print (obj, " (");
                    px (out, args);
                    out.print (")");
                }
            }
        );
    }

    @Override
    public JExpr                    callThis (final JExpr ... args) {
        return call(accessThis(), args);
    }

    @Override
    public JExpr                    staticCall (final JExpr... args) {
        return (
            new JExprImplBase (context) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print (containerClass ().fullName (), ".", name (), " (");
                    px (out, args);
                    out.print (")");
                }
            }
        );
    }

    private JExpr            accessThis () {
        return (
            new JExprImplBase (context) {
                @Override
                public void print (int outerPriority, SourceCodePrinter out) throws IOException {
                    final JClass containerClass = containerClass();
                    if (containerClass == null)
                        out.print (name ());
                    else if (containerClass == out.currentClass)
                        out.print ("this.", name ());
                    else if (Modifier.isStatic (modifiers ()))
                        out.print (containerClass.fullName (), ".", name ());
                    else
                        out.print (containerClass.name (), ".this.", name ());
                }
            }
        );
    }

    @Override
    public void             addAnnotation (JAnnotation annotation) {
        annotations.add (annotation);
    }
}
