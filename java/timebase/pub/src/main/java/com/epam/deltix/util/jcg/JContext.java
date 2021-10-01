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

import com.epam.deltix.util.jcg.scg.JTypeImpl;

/**
 *
 */
public interface JContext {
    JCompoundStatement   compStmt ();

    JClass       newClass (
        int                 modifiers,
        String              packageName,
        String              simpleName,
        JClass              parent
    );

    JClass       newClass (
        int                 modifiers,
        String              packageName,
        String              simpleName,
        Class <?>           parent
    );

    JType        asType (Class <?> cls);

    JExpr        instanceOf (JExpr expr, Class <?> toClass);

    JExpr        instanceOf (JExpr expr, JType toType);

    JType        arrayTypeOf (Class <?> cls);

    JType        arrayTypeOf (JType type);

    JExpr        arrayLength (JExpr array);

    JExpr        condExpr (JExpr cond, JExpr pos, JExpr neg);

    JExpr        staticVarRef (Class <?> cls, String fieldName);

    JExpr        staticVarRef (JClass cls, String fieldName);

    JExpr        staticVarRef (String cls, String fieldName);

    JExpr        localVarRef(final String fieldName);

    JStatement   ifStmt (JExpr cond, JStatement then, JStatement els);
    
    JStatement   ifStmt (JExpr cond1, JStatement then1, JExpr cond2, JExpr then2, JStatement els);

    JStatement   ifStmt (JExpr left, JExpr right, JStatement bothTrue, JStatement leftTrue, JStatement rightTrue, JStatement bothFalse);

    default JStatement ifStmt (JExpr left, JExpr right, JExpr bothTrue, JExpr leftTrue, JExpr rightTrue, JExpr bothFalse) {
        return ifStmt(left, right, bothTrue.asStmt(), leftTrue.asStmt(), rightTrue.asStmt(), bothFalse.asStmt());
    }

    JStatement   ifStmt (JExpr cond, JStatement then);

    JStatement   returnStmt ();

    JStatement   breakStmt ();

    JStatement   breakStmt (String label);

    JStatement   continueStmt();

    JExpr        binExpr (JExpr left, String op, JExpr right);

    JExpr        conjunction(JExpr ... args);

    JExpr        disjunction(JExpr ... args);

    JExpr        sum (JExpr ... es);

    JExpr        call (String method, JExpr ... args);

    JExpr        staticCall (Class <?> cls, String method, JExpr ... args);

    JExpr        staticCall (Class <?> cls, String method, Class <?> typeArgument, JExpr ... args);
    
    JExpr        newExpr (Class <?> cls, JExpr ... args);

    JExpr        newGenericExpr(Class<?> cls, JExpr ... args);

    JExpr        supplierWithNew(Class<?> cls, JExpr ... args);

    JExpr        newArrayExpr (Class<?> cls, JExpr... elements);

    JExpr        newExpr (JType type, JExpr ... args);

    JExpr        nullLiteral ();

    JExpr        thisLiteral ();

    JExpr        booleanLiteral (boolean value);

    JExpr        trueLiteral ();

    JExpr        falseLiteral ();

    JExpr        charLiteral (char value);

    JExpr        floatLiteral (float value);

    JExpr        doubleLiteral (double value);

    JExpr        intLiteral (int value);

    JExpr        longLiteral (long value);

    JExpr        stringLiteral (String value);

    JExpr        classLiteral (Class<?> value);

    JExpr        classLiteral (JClass value);

    JExpr        enumLiteral (Enum<?> value);

    JExpr        enumLiteral (Object value);

    JTryStatement tryStmt ();

    JStatement           assertStmt (JExpr passCond, JExpr message);

    JArrayInitializer    arrayInitializer (Class<?> type);

    JArrayInitializer    arrayInitializer (JType type);

    JExpr                typeof (Class<?> type);

    JExpr                typeof (JClass type);

    JStatement           forStmt (JExpr init, JExpr condition, JExpr update, JStatement body);

    /**
     *
     * @param clazz - the class of Annotation
     * @param args -  Elements of the Annotation. Even elements are the keys. Odd elements are the values.
     * One element if only value() parameter exists.
     * @return JAnnotation's instance
     * <br>
     * <br>
     * <b>Example:</b>
     * <br>
     * <br>
     * Annotation
     * <pre>
     *  public @interface TODO {<br>
     *       String author ();<br>
     *       String date ();<br>
     *       int currentRevision ();<br>
     *       String value ();<br>
     *  }<br>
     *</pre>
     * will be created as <CODE>JContext.annotation (TODO.class, "author", "Author Name", "date", "2010-01-01", "currentRevision",  1, "value", "SOME VALUE")</CODE>
     * <br>
     * <br>
     * Annotation
     * <pre>
     *  public @interface MappedTo {<br>
     *       String value ();<br>
     *  }<br>
     *</pre>
     * will be created as <CODE>JContext.annotation ("TE$$T")</CODE>
     */
    JAnnotation          annotation (Class<?> clazz, Object... args);

    JAnnotation          annotation (JTypeImpl clazz, Object... args);
}