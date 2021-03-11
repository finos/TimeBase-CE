package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.JClass;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JInitMemberVariable;

import java.io.IOException;

public class ThisVariableImpl extends InitVariableImpl
    implements JInitMemberVariable, JMemberIntf
{
    private JClass containerClass;

    ThisVariableImpl (ClassImpl containerClass) {
        super (containerClass.context, 0, null, null);
        this.containerClass = containerClass;
    }

    @Override
    public JClass containerClass() {
        return containerClass;
    }

    @Override
    public JExpr access () {
        return (
            new JExprImplBase (context) {
                @Override
                public void print (int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print ("this");
                }
            }
        );
    }

    @Override
    public JExpr            access (final JExpr obj) {
        if (obj == null)
            throw new IllegalArgumentException ("null object");

        return (
            new JExprImplBase (context) {
                @Override
                public void print (int outerProprity, SourceCodePrinter out) throws IOException {
                    printPostfix (
                        outerProprity, 
                        (JExprImplBase) obj, 
                        ".this", JavaOpPriority.POSTFIX, 
                        out
                    );
                }
            }
        );
    }

    @Override
    public void             print (int outerProprity, SourceCodePrinter out)
        throws IOException
    {
        throw new RuntimeException ("should not be called");
    }

    @Override
    public void addComment(String comment) {

    }
}
