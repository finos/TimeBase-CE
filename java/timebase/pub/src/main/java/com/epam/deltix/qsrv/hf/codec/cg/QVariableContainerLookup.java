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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.jcg.scg.ClassImpl;

import java.lang.reflect.Modifier;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 * Adds variable lookup to QVariableContainer
 */
public class QVariableContainerLookup extends QVariableContainer {

    private final ObjectToObjectHashMap<String, JVariable> map = new ObjectToObjectHashMap<String, JVariable>();
    private final ObjectToObjectHashMap<String, JClass> mapInnerClasses = new ObjectToObjectHashMap<>();
    private final ObjectToObjectHashMap<String, JMethod> accessors = new ObjectToObjectHashMap<>();

    public QVariableContainerLookup(int modifiers, JVariableContainer container) {
        super(modifiers, container, null, "f_");
    }

    public JVariable addVar (JType type, String name) {
        return (addVar (type, name, null));
    }

    public JExpr addAccessor (JType type, String name, JMemberVariable var, JStatement initializer) {
        if (container instanceof ClassImpl) {
            JMethod method = ((ClassImpl) container).addMethod(Modifier.PRIVATE, type, name);
            JCompoundStatement body = method.body();

            body.add(CTXT.ifStmt(CTXT.binExpr(var.access(), "==", CTXT.nullLiteral()), initializer));
            body.add(var.access().returnStmt());

            accessors.put(name, method);
            return method.callThis();
        }

        throw new IllegalStateException("Cannot add accessor for the " + container);
    }

    public static String    getAccessorName(String name) {
        return "get" + String.valueOf(name.charAt(0)).toUpperCase() + name.substring(1);
    }

    public JExpr addAccessor (Class <?> type, String name, JMemberVariable var, JStatement initializer) {
        return addAccessor(CTXT.asType(type), name, var, initializer);
    }

    public JVariable addVar (Class <?> type, String name) {
        return (addVar (type, name, null));
    }

    public JVariable            addVar (Class <?> type, String name, JExpr initValue) {
        return addVar(CTXT.asType(type), name, initValue);
    }

    public JVariable            addVar(int modifiers, Class <?> type, String name) {
        return addVar(modifiers, CTXT.asType(type), name);
    }

    public JVariable            addVar(int modifiers, JType type, String name) {
        if (map.containsKey(name))
            throw new IllegalArgumentException("container has already a variable with the same name " + name);
        else {
            final JVariable var = container.addVar(modifiers, type, name, null);
            map.put(name, var);
            return (var);
        }
    }

    public JVariable            addVar (JType type, String name, JExpr initValue) {
        if (map.containsKey(name))
            throw new IllegalArgumentException("container has already a variable with the same name " + name);
        else {
            final JVariable var = container.addVar(modifiers, type, name, initValue);
            map.put(name, var);
            return (var);
        }
    }

    public JExpr                lookupAccessor(String name) {
        JMethod accessor = null;
        if (!accessors.containsKey(name) && container instanceof ClassImpl)
            accessor = ((ClassImpl)container).getMethod(name);
        else
            accessor = accessors.get(name, null);

        return accessor != null ? accessor.callThis() : null;
    }

    public JVariable lookupVar(String name) {
        if (!map.containsKey(name) && container instanceof ClassImpl)
            return ((ClassImpl)container).getVar(name);

        return map.get(name, null);
    }

    public JCompoundStatement getInitStmt() {
        if (container instanceof ClassImpl)
            return ((ClassImpl)container).getConstructor().body();
        else if (container instanceof JCompoundStatement)
            return (JCompoundStatement) container;
        else
            throw new UnsupportedOperationException("unsupported container type " + container.getClass().getSimpleName());
    }

    public JClass lookupInnerClass(String name) {
        return mapInnerClasses.get(name, null);
    }

    public JClass addInnerClass(
            int modifiers,
            String simpleName,
            Class<?> parent
    ) {
        if (container instanceof JClass) {
            final JClass innerClass = ((JClass) container).innerClass(modifiers, simpleName, parent);
            mapInnerClasses.put(simpleName, innerClass);
            return innerClass;
        } else
            throw new UnsupportedOperationException("unsupported container type " + container.getClass().getSimpleName());
    }
}
