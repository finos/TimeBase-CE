package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JCallable extends JMember {
    public JCompoundStatement   body ();

    public JMethodArgument      addArg (
        int                         modifiers,
        JType                       type,
        String                      name
    );

    public JMethodArgument      addArg (
        int                         modifiers,
        Class <?>                   type,
        String                      name
    );
}
