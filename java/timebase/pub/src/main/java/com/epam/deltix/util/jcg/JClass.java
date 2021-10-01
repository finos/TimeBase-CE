/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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

    public JInitMemberVariable  addVar(
            int                     modifiers,
            JType                   type,
            JType[]                 typeArgs,
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