package com.epam.deltix.util.jcg;

import com.epam.deltix.util.jcg.scg.JTypeImpl;

/**
 *
 */
public interface JContext {
    public JCompoundStatement   compStmt ();

    public JClass       newClass (
        int                 modifiers,
        String              packageName,
        String              simpleName,
        JClass              parent
    );

    public JClass       newClass (
        int                 modifiers,
        String              packageName,
        String              simpleName,
        Class <?>           parent
    );

    public JType        asType (Class <?> cls);

    public JExpr        instanceOf (JExpr expr, Class <?> toClass);

    public JExpr        instanceOf (JExpr expr, JType toType);

    public JType        arrayTypeOf (Class <?> cls);

    public JType        arrayTypeOf (JType type);

    public JExpr        arrayLength (JExpr array);

    public JExpr        condExpr (JExpr cond, JExpr pos, JExpr neg);

    public JExpr        staticVarRef (Class <?> cls, String fieldName);

    public JExpr        staticVarRef (JClass cls, String fieldName);

    public JExpr        staticVarRef (String cls, String fieldName);

    public JStatement   ifStmt (JExpr cond, JStatement then, JStatement els);

    public JStatement   ifStmt (JExpr cond, JStatement then);

    public JStatement   returnStmt ();

    public JStatement   breakStmt ();

    public JStatement   breakStmt (String label);

    public JExpr        binExpr (JExpr left, String op, JExpr right);

    public JExpr        sum (JExpr ... es);

    public JExpr        call (String method, JExpr ... args);

    public JExpr        staticCall (Class <?> cls, String method, JExpr ... args);

    public JExpr        staticCall (Class <?> cls, String method, Class <?> typeArgument, JExpr ... args);
    
    public JExpr        newExpr (Class <?> cls, JExpr ... args);

    JExpr               newArrayExpr (Class<?> cls, JExpr... elements);

    public JExpr        newExpr (JType type, JExpr ... args);

    public JExpr        nullLiteral ();

    public JExpr        booleanLiteral (boolean value);

    public JExpr        trueLiteral ();

    public JExpr        falseLiteral ();

    public JExpr        charLiteral (char value);

    public JExpr        floatLiteral (float value);

    public JExpr        doubleLiteral (double value);

    public JExpr        intLiteral (int value);

    public JExpr        longLiteral (long value);

    public JExpr        stringLiteral (String value);

    public JExpr        classLiteral (Class<?> value);

    public JExpr        classLiteral (JClass value);

    public JExpr        enumLiteral (Enum<?> value);

    public JExpr        enumLiteral (Object value);

    public JTryStatement tryStmt ();

    public JStatement           assertStmt (JExpr passCond, JExpr message);

    public JArrayInitializer    arrayInitializer (Class<?> type);

    public JArrayInitializer    arrayInitializer (JType type);

    public JExpr                typeof (Class<?> type);

    public JExpr                typeof (JClass type);

    public JStatement           forStmt (JExpr init, JExpr condition, JExpr update, JStatement body);

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
    public JAnnotation          annotation (Class<?> clazz, Object... args);

    public JAnnotation          annotation (JTypeImpl clazz, Object... args);
}
