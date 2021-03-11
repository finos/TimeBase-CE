package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.*;
import java.lang.reflect.Modifier;

/**
 *
 */
public class QVariableContainer {
    protected final int                 modifiers;
    protected final JVariableContainer  container;
    private final String                prefix;
    private final JExpr                 accessExpr;
    private int                         counter = 1;

    public QVariableContainer (
        int                     modifiers,
        JVariableContainer      container,
        JExpr                   accessExpr,
        String                  prefix
    )
    {
        this.modifiers = modifiers;
        this.container = container;
        this.accessExpr = accessExpr;
        this.prefix = prefix;
    }

    public JExpr                access (JVariable v) {
        if (v instanceof JLocalVariable)
            return ((JLocalVariable) v);
        else if (v instanceof JMemberVariable) {
            JMemberVariable     mv = (JMemberVariable) v;

            return (accessExpr == null ? mv.access () : mv.access (accessExpr));
        }
        else
            throw new RuntimeException (v.getClass ().getSimpleName ());
    }

    public JVariable            addVar (
        String                      comment,
        boolean                     forceFinal, 
        Class <?>                   type, 
        JExpr                       initValue
    )
    {
        int         m = modifiers;
        
        if (forceFinal)
            m |= Modifier.FINAL;
        
        if (comment != null)
            container.addComment (comment);
                
        return (container.addVar (m, type, prefix + (counter++), initValue));
    }
}
