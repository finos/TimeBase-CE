package com.epam.deltix.util.jcg;

import java.util.List;

/**
 *
 */
public interface JClass extends JMember, JType, JVariableContainer, JAnnotationContainer {
    /**
     *  Returns the package name of this class, or null if top-level package.
     */
    public String           packageName ();
    
    public String           fullName ();

    public void             addImplementedInterface (Class <?> cls);

    public void             addImplementedInterface (JClass cls);
    
    public JClass           innerClass (
        int                     modifiers,
        String                  simpleName,
        JClass                  parent
    );

    public JClass           innerClass (
        int                     modifiers,
        String                  simpleName,
        Class <?>               parent
    );
    
    public JClass           innerClass (
        int                     modifiers,
        String                  simpleName
    );

    public JConstructor     addConstructor (int modifiers);

    public JMethod          addMethod (
        int                     modifiers,
        JType                   type,
        String                  name
    );

    public JMethod          addMethod (
        int                     modifiers,
        Class <?>               type,
        String                  name
    );

    public JInitMemberVariable  addVar (
        int                     modifiers,
        JType                   type,
        String                  name
    );

    public JInitMemberVariable  addVar (
        int                     modifiers,
        JType                   type,
        String                  name,
        JExpr                   initValue
    );

    public JInitMemberVariable  addVar(
        int                     modifiers,
        JType                   type,
        String                  name,
        JExpr                   initValue,
        boolean                 nullable
    );

    public JInitMemberVariable  addVar (
        int                     modifiers,
        Class <?>               type,
        String                  name
    );

    public JInitMemberVariable  addVar (
        int                     modifiers,
        Class <?>               type,
        String                  name,
        JExpr                   initValue
    );

    public JMemberVariable      addProperty (
            int                     modifiers,
            Class <?>               type,
            String                  name
    );

    public JMemberVariable      getVar(String name);

    public List<JMemberVariable> getVars();

    public JMemberVariable      inheritedVar (String name);

    public JMemberVariable      thisVar();

    public JExpr                newExpr (JExpr ... args);

    public JExpr                callSuperMethod (String name, JExpr ... args);
}
