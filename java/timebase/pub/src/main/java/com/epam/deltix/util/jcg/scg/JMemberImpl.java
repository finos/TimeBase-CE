package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.JClass;

/**
 *
 */
abstract class JMemberImpl implements JMemberIntf {
    protected final JContextImpl    context;
    private final int               modifiers;
    private final String            name;
    private final JClass            container;
    
    JMemberImpl (
        int                 modifiers,
        String              name,
        ClassImpl           container
    )
    {
        this.context = container.context;
        this.modifiers = modifiers;
        this.name = name;
        this.container = container;
    }

    JMemberImpl (
        JContextImpl        context,
        int                 modifiers,
        String              name
    )
    {
        this.context = context;
        this.modifiers = modifiers;
        this.name = name;
        this.container = null;
    }

    @Override
    public final String     name () {
        return (name);
    }

    @Override
    public final int        modifiers () {
        return modifiers;
    }

    @Override
    public final JClass     containerClass () {
        return container;
    }          
}
