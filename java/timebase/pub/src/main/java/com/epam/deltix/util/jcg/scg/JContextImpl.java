/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.lang.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 *  Contains code common for Java and C# implementations.
 */
public abstract class JContextImpl implements JContext {
    public static String    cn (Class <?> cls) {
        if (cls == null)
            return (null);

        return  cls.getCanonicalName();
    }

    String cn1(Class<?> cls) {
        if (cls == null)
            return (null);

        String name = cls.getCanonicalName();
        name = translateType(name);

        return name;
    }

    String cn1(JType type, boolean nullable) {
        return cn(type) + (nullable ? "?" : "");
    }

    abstract String           translateType(String type);

    public static String [] cn (Class <?> [] classes) {
        if (classes == null)
            return (null);

        int     n = classes.length;

        if (n == 0)
            return (null);

        String [] cns = new String [n];

        for (int ii = 0; ii < n; ii++)
            cns [ii] = cn (classes [ii]);

        return (cns);
    }

    public static String    cn (JType cls) {
        if (cls == null)
            return (null);

        final String name = ((JTypeImpl) cls).fullName ();
        return name.startsWith("cli.") ? name.substring(4) : name;
    }

    public static String [] cn (JType [] classes) {
        if (classes == null)
            return (null);

        int     n = classes.length;

        if (n == 0)
            return (null);

        String [] cns = new String [n];

        for (int ii = 0; ii < n; ii++)
            cns [ii] = cn (classes [ii]);

        return (cns);
    }

    JType            classToType (final Class <?> cls) {
        return (
                new JTypeImpl () {
                    @Override
                    public String fullName() {
                        return cls.getCanonicalName();
                    }
                }
        );
    }

    JType[] classesToType(final Class<?>[] classes) {
        return Arrays.stream(classes).map(this::classToType).toArray(JType[]::new);
    }

    static JType            typeToArray (final JType type) {
        return (
                new JTypeImpl () {
                    @Override
                    public String fullName () {
                        return (((JTypeImpl) type).fullName() + "[]");
                    }
                }
        );
    }

    static JStatement etos (final JExprImplBase e) {
        return (
                new JStatementImplBase (e.context) {
                    @Override
                    public void     printElement (SourceCodePrinter out) throws IOException {
                        out.print (e, ";");
                    }
                }
        );
    }

    JExpr                   mkint (final int value) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print (value);
                    }
                }
        );
    }

    JStatement              mkbreak (final String label) {
        return (
                new JStatementImplBase (this) {
                    @Override
                    public void     printElement (SourceCodePrinter out) throws IOException {
                        out.print ("break");

                        if (label != null)
                            out.print (" ", label);

                        out.print (";");
                    }
                }
        );
    }
    //
    //  JContext implementation
    //
    @Override
    public JType        asType (Class <?> cls) {
        return (classToType (cls));
    }

    @Override
    public JType        arrayTypeOf (Class <?> cls) {
        return (typeToArray (classToType (cls)));
    }

    @Override
    public JType        arrayTypeOf (JType type) {
        return (typeToArray (type));
    }

    @Override
    public JExpr arrayLength(JExpr array) {
        return array.field("length");
    }

    @Override
    public ClassImpl    newClass (
            int                 modifiers,
            String              packageName,
            String              simpleName,
            Class <?>           parent
    )
    {
        return (newClassImpl (modifiers, packageName, simpleName, cn (parent)));
    }

    @Override
    public ClassImpl       newClass (
            int                 modifiers,
            String              packageName,
            String              simpleName,
            JClass              parent
    )
    {
        return (newClassImpl (modifiers, packageName, simpleName, cn (parent)));
    }

    @Override
    public JExpr        intLiteral (final int value) {
        return (mkint (value));
    }

    @Override
    public JExpr        longLiteral (final long value) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print (value, "L");
                    }
                }
        );
    }

    @Override
    public JExpr        floatLiteral (final float value) {
        if (Float.isNaN(value))
            return staticVarRef(Float.class, "NaN");
        return (
                new JExprImplBase(this) {
                    @Override
                    public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print(value, "F");
                    }
                }
        );
    }

    @Override
    public JExpr        doubleLiteral (final double value) {
        if (Double.isNaN(value))
            return staticVarRef(Double.class, "NaN");
        else
            return (
                    new JExprImplBase(this) {
                        @Override
                        public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                            out.print(value);
                        }
                    }
            );
    }

    @Override
    public JExpr        stringLiteral (final String value) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print (StringUtils.escapeJavaStringLiteral (value));
                    }
                }
        );
    }

    @Override
    public JExpr        nullLiteral () {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print ("null");
                    }
                }
        );
    }

    @Override
    public JExpr thisLiteral() {
        return (
                new JExprImplBase(this) {
                    @Override
                    public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print("this");
                    }
                }
        );
    }

    @Override
    public JExpr        trueLiteral () {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print ("true");
                    }
                }
        );
    }

    @Override
    public JExpr        falseLiteral () {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print ("false");
                    }
                }
        );
    }

    @Override
    public JExpr charLiteral(final char value) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print("'", StringUtils .escapeJavaString(String.valueOf(value)), "'");
                    }
                }
        );
    }

    @Override
    public JExpr        enumLiteral (final Enum<?> value) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print(cn1(value.getClass()), '.', value);
                    }
                }
        );
    }

    @Override
    public JExpr        condExpr (final JExpr cond, final JExpr pos, final JExpr neg) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        boolean             parenthesize = outerPriority > JavaOpPriority.TERNARY;

                        if (parenthesize)
                            out.print ("(");

                        ((JExprImplBase) cond).print (JavaOpPriority.TERNARY + 1, out);
                        out.print (" ? ");
                        ((JExprImplBase) pos).print (JavaOpPriority.TERNARY + 1, out);
                        out.print (" : ");
                        //
                        //  force parentheses around chained ? ops to prevent confusion
                        //  formally JavaOpPriority.TERNARY would be sufficient.
                        //
                        ((JExprImplBase) neg).print (JavaOpPriority.TERNARY + 1, out);

                        if (parenthesize)
                            out.print (")");
                    }
                }
        );
    }

    @Override
    public JStatement   ifStmt (
            final JExpr         cond,
            final JStatement    then
    )
    {
        return (ifStmt (cond, then, null));
    }

    @Override
    public JStatement ifStmt(List<JExpr> cond, List<JStatement> then, JStatement els) {
        return new JStatementImplBase(this) {
            @Override
            public void printElement(SourceCodePrinter out) throws IOException {
                out.print("if (", cond.get(0), ")");
                out.newLine();
                if (then.get(0) instanceof JCompoundStatement) {
                    ((JStatementImplBase) then.get(0)).printElement(out);
                } else {
                    out.print("{");
                    out.indent(+1);
                    ((JStatementImplBase) then.get(0)).printElement(out);
                    out.indent(-1);
                    out.print("}");
                }

                for (int i = 1; i < cond.size(); ++i) {
                    out.newLine();
                    out.print("else if (", cond.get(i), ")");
                    out.newLine();

                    if (then.get(1) instanceof JCompoundStatement)
                        ((JStatementImplBase) then.get(i)).printElement(out);
                    else {
                        out.print("{");
                        out.indent(+1);
                        ((JStatementImplBase) then.get(i)).printElement(out);
                        out.indent(-1);
                        out.print("}");
                    }
                }

                if (els != null) {
                    out.newLine();
                    out.print("else");
                    out.newLine();
                    if (els instanceof JCompoundStatement)
                        ((JStatementImplBase) els).printElement(out);
                    else {
                        out.print("{");
                        out.indent(+1);
                        ((JStatementImplBase) els).printElement(out);
                        out.indent(-1);
                        out.print("}");
                    }
                }
            }
        };
    }

    @Override
    public JStatement   ifStmt (
            final JExpr         cond,
            final JStatement    then,
            final JStatement    els
    )
    {
        return (
                new JStatementImplBase (this) {
                    @Override
                    public void     printElement (SourceCodePrinter out) throws IOException {
                        out.print ("if (", cond, ")");
                        out.newLine();
                        if (then instanceof JCompoundStatement)
                            ((JStatementImplBase) then).printElement(out);
                        else {
                            out.indent(+1);
                            ((JStatementImplBase) then).printElement(out);
                            out.indent(-1);
                        }

                        if (els != null) {
                            out.newLine ();
                            out.print ("else");
                            out.newLine ();
                            if (els instanceof JCompoundStatement)
                                ((JStatementImplBase) els).printElement (out);
                            else {
                                out.indent(+1);
                                ((JStatementImplBase) els).printElement (out);
                                out.indent(-1);
                            }
                        }
                    }
                }
        );
    }

    @Override
    public JStatement ifStmt(JExpr cond1, JStatement then1, JExpr cond2, JExpr then2, JStatement els) {
        return new JStatementImplBase(this) {
            @Override
            public void printElement(SourceCodePrinter out) throws IOException {
                out.print ("if (", cond1, ")");
                out.newLine();
                if (then1 instanceof JCompoundStatement)
                    ((JStatementImplBase) then1).printElement(out);
                else {
                    out.indent(+1);
                    ((JStatementImplBase) then1).printElement(out);
                    out.indent(-1);
                }

                out.print("else if (", cond2, ")");
                out.newLine();

                if (then2 instanceof JCompoundStatement)
                    ((JStatementImplBase) then2).printElement(out);
                else {
                    out.indent(+1);
                    ((JStatementImplBase) then2).printElement(out);
                    out.indent(-1);
                }

                if (els != null) {
                    out.newLine ();
                    out.print ("else");
                    out.newLine ();
                    if (els instanceof JCompoundStatement)
                        ((JStatementImplBase) els).printElement (out);
                    else {
                        out.indent(+1);
                        ((JStatementImplBase) els).printElement (out);
                        out.indent(-1);
                    }
                }
            }
        };
    }

    @Override
    public JStatement ifStmt(JExpr left, JExpr right, JStatement bothTrue, JStatement leftTrue, JStatement rightTrue, JStatement bothFalse) {
        return new JStatementImplBase(this) {
            @Override
            public void printElement(SourceCodePrinter out) throws IOException {
                out.print ("if (", binExpr(left, "&&", right), ")");
                out.newLine();
                if (bothTrue instanceof JCompoundStatement)
                    ((JStatementImplBase) bothTrue).printElement(out);
                else {
                    out.indent(+1);
                    ((JStatementImplBase) bothTrue).printElement(out);
                    out.indent(-1);
                }

                out.print("else if (", left, ")");
                out.newLine();

                if (leftTrue instanceof JCompoundStatement)
                    ((JStatementImplBase) leftTrue).printElement(out);
                else {
                    out.indent(+1);
                    ((JStatementImplBase) leftTrue).printElement(out);
                    out.indent(-1);
                }

                out.print("else if (", right, ")");
                out.newLine();

                if (rightTrue instanceof JCompoundStatement)
                    ((JStatementImplBase) rightTrue).printElement(out);
                else {
                    out.indent(+1);
                    ((JStatementImplBase) rightTrue).printElement(out);
                    out.indent(-1);
                }

                if (bothFalse != null) {
                    out.newLine ();
                    out.print ("else");
                    out.newLine ();
                    if (bothFalse instanceof JCompoundStatement)
                        ((JStatementImplBase) bothFalse).printElement (out);
                    else {
                        out.indent(+1);
                        ((JStatementImplBase) bothFalse).printElement (out);
                        out.indent(-1);
                    }
                }
            }
        };
    }

    @Override
    public JExpr        binExpr (final JExpr left, final String op, final JExpr right) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        int                 p;
                        InfixAssociation    a = InfixAssociation.LEFT;

                        switch (op) {
                            case "*":
                            case "/":
                            case "%":
                                p = JavaOpPriority.MULTIPLICATIVE;
                                break;

                            case "+":
                            case "-":
                                p = JavaOpPriority.ADDITIVE;
                                break;

                            case "<<":
                            case ">>":
                            case ">>>":
                                p = JavaOpPriority.SHIFT;
                                break;

                            case "<":
                            case ">":
                            case "<=":
                            case ">=":
                                p = JavaOpPriority.RELATIONAL;
                                break;

                            case "==":
                            case "!=":
                                p = JavaOpPriority.EQUALITY;
                                break;

                            case "&":
                                p = JavaOpPriority.BIT_AND;
                                break;

                            case "^":
                                p = JavaOpPriority.BIT_XOR;
                                break;

                            case "|":
                                p = JavaOpPriority.BIT_OR;
                                break;

                            case "&&":
                                p = JavaOpPriority.BOOL_AND;
                                break;

                            case "||":
                                p = JavaOpPriority.BOOL_OR;
                                break;

                            case "=":
                            case "+=":
                            case "-=":
                            case "*=":
                            case "/=":
                            case "%=":
                            case "&=":
                            case "|=":
                            case "^=":
                            case "<<=":
                            case ">>=":
                            case ">>>=":
                                p = JavaOpPriority.ASSIGNMENT;
                                break;

                            default:
                                p = JavaOpPriority.OPEN;
                                break;
                        }

                        printBinary (
                                outerPriority,
                                (JExprImplBase) left,
                                op, p, a,
                                (JExprImplBase) right,
                                out
                        );
                    }
                }
        );
    }

    @Override
    public JExpr conjunction(JExpr... args) {
        return new JExprImplBase(this) {
            @Override
            public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                int p = JavaOpPriority.BOOL_AND;
                out.print("(");
                ((JExprImplBase) args[0]).print(p, out);
                for (int i = 1; i < args.length; i++) {
                    out.print(" && ");
                    ((JExprImplBase) args[1]).print(p, out);
                }
                out.print(")");
            }
        };
    }

    @Override
    public JExpr disjunction(JExpr... args) {
        return new JExprImplBase(this) {
            @Override
            public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                int p = JavaOpPriority.BOOL_AND;
                out.print("(");
                ((JExprImplBase) args[0]).print(p, out);
                for (int i = 1; i < args.length; i++) {
                    out.print(" || ");
                    ((JExprImplBase) args[1]).print(p, out);
                }
                out.print(")");
            }
        };
    }

    @Override
    public JExpr        sum (JExpr ... es) {
        JExpr   ret = null;

        for (JExpr e : es)
            if (ret == null)
                ret = e;
            else
                ret = binExpr (ret, "+", e);

        if (ret == null)
            throw new IllegalArgumentException ("No args");

        return (ret);
    }

    @Override
    public JExpr        staticVarRef (final Class <?> cls, final String fieldName) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.printRefClassName (cn (cls));
                        out.print (".", fieldName);
                    }
                }
        );
    }

    @Override
    public JExpr        staticVarRef (final String cls, final String fieldName) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print (cls);
                        out.print (".", fieldName);
                    }
                }
        );
    }

    @Override
    public JExpr localVarRef(final String fieldName) {
        return (
                new JExprImplBase(this) {
                    @Override
                    public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print(fieldName);
                    }
                }
        );
    }

    @Override
    public JExpr        staticVarRef (final JClass cls, final String fieldName) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.printRefClassName (cn (cls));
                        out.print (".", fieldName);
                    }
                }
        );
    }

    static void         px (SourceCodePrinter out, final JExpr [] args) throws IOException {
        for (int ii = 0; ii < args.length; ii++) {
            if (ii > 0)
                out.print (", ");

            out.print (args [ii]);
        }
    }

    @Override
    public JExpr        staticCall (final Class <?> cls, final String method, final JExpr... args) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.printRefClassName (cn (cls));
                        out.print (".", method, " (");
                        px (out, args);
                        out.print (")");
                    }
                }
        );
    }

    @Override
    public JExpr        call (final String method, final JExpr... args) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print (method, " (");
                        px (out, args);
                        out.print (")");
                    }
                }
        );
    }

    @Override
    public JStatement   returnStmt () {
        return (
                new JStatementImplBase (this) {
                    @Override
                    public void     printElement (SourceCodePrinter out) throws IOException {
                        out.print ("return;");
                    }
                }
        );
    }

    @Override
    public JStatement   breakStmt () {
        return (breakStmt (null));
    }

    @Override
    public JStatement   breakStmt (final String label) {
        return (mkbreak (label));
    }

    @Override
    public JStatement continueStmt() {
        return new JStatementImplBase (this) {
            @Override
            public void printElement (SourceCodePrinter out) throws IOException {
                out.print ("continue;");
            }
        };
    }

    @Override
    public JExpr newGenericExpr(Class<?> cls, JExpr... args) {
        return new JExprImplBase(this) {
            @Override
            void print(int outerPriority, SourceCodePrinter out) throws IOException {
                out.print("new ");
                out.printRefClassName (cn (cls));
                out.print("<>(");
                px(out, args);
                out.print(")");
            }
        };
    }

    @Override
    public JExpr supplierWithNew(Class<?> cls, JExpr... args) {
        return new JExprImplBase(this) {
            @Override
            void print(int outerPriority, SourceCodePrinter out) throws IOException {
                out.print("() -> new ");
                if (cls.isArray()) {
                    if (args.length > 2)
                        throw new IllegalArgumentException("invalid number of arguments for array constructor " + args.length);
                    out.printRefClassName(cls.getComponentType().getCanonicalName());
                    out.print("[");
                    // TODO: That is a hack for initializer case
                    if (args.length == 1)
                        out.print(args[0]);
                    out.print("]");
                    if (args.length == 2)
                        out.print(args[1]);
                } else {
                    out.printRefClassName (cn (cls));
                    out.print(" (");
                    px(out, args);
                    out.print(")");
                }
            }
        };
    }

    @Override
    public JExpr        newExpr (final Class <?> cls, final JExpr ... args) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        //TODO: create new method for arrays newArrayExpr()
                        if (cls.isArray()) {
                            if (args.length > 2)
                                throw new IllegalArgumentException("invalid number of arguments for array constructor " + args.length);
                            out.print("new ");
                            out.printRefClassName(cls.getComponentType().getCanonicalName());
                            out.print("[");
                            // TODO: That is a hack for initializer case
                            if (args.length == 1)
                                out.print(args[0]);
                            out.print("]");
                            if (args.length == 2)
                                out.print(args[1]);
                        } else {
                            out.print("new ");
                            out.printRefClassName (cn (cls));
                            out.print(" (");
                            px(out, args);
                            out.print(")");
                        }
                    }
                }
        );
    }

    @Override
    public JExpr        newArrayExpr (final Class <?> cls, final JExpr ... elements) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print("new ");
                        out.printRefClassName (cn (cls));
                        out.print("[]");
                        if (elements.length == 1 && elements[0] instanceof JArrayInitializer) {
                            out.print(elements[0]);
                        } else {
                            out.print("{");
                            for (int i = 0; i < elements.length; i++) {
                                if (i > 0) out.print(",");
                                out.print(elements[i]);
                            }
                            out.print("}");
                        }
                    }
                }
        );
    }

    @Override
    public JExpr        newExpr (final JType type, final JExpr ... args) {
        return (
                new JExprImplBase (this) {
                    @Override
                    public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                        out.print("new ");
                        out.printRefClassName (cn(type));
                        out.print(" (");
                        px(out, args);
                        out.print(")");
                    }
                }
        );
    }

    @Override
    public JCompoundStatement compStmt () {
        return (new JCompStmtImpl (this));
    }

    @Override
    public JExpr        booleanLiteral (boolean value) {
        return (value ? trueLiteral () : falseLiteral ());
    }

    @Override
    public JTryStatement tryStmt() {
        return new JTryStatementImpl(this);
    }

    @Override
    public JArrayInitializer arrayInitializer (Class<?> type) {
        return arrayInitializer (classToType (type));
    }

    @Override
    public JArrayInitializer arrayInitializer (JType type) {
        return new JArrayInitializerImpl (this);
    }

    @Override
    public JStatement forStmt (
            final JExpr         init,
            final JExpr         condition,
            final JExpr         update,
            final JStatement    body
    )
    {
        return (
                new JStatementImplBase (this) {
                    @Override
                    public void     printElement (SourceCodePrinter out) throws IOException {
                        out.print ("for (");

                        if (init != null)
                            out.print (init);

                        out.print ("; ");

                        if (condition != null)
                            out.print (condition);

                        out.print ("; ");

                        if (update != null)
                            out.print (update);

                        out.println (")");

                        out.indent (+1);
                        ((JStatementImplBase) body).printElement (out);
                        out.indent (-1);
                    }
                }
        );
    }

    protected abstract void     printModifiers (int mods, SourceCodePrinter out)
            throws IOException;

    protected abstract void     printType(String type, SourceCodePrinter out)
            throws IOException;

    protected abstract void     printType(String type, String[] typeArgs, SourceCodePrinter out)
            throws IOException;

    abstract ClassImpl          newClassImpl (
            int                         modifiers,
            String                      packageName,
            String                      simpleName,
            String                      parentName
    );

    int                         refineModifiersForLocalVarDecl (int mods) {
        return (mods);
    }

    int                         refineModifiersForMethodArg (int mods) {
        return (mods);
    }
}