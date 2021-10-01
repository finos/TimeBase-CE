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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.BinaryDataType;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.QueryDataType;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.StdEnvironment;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.FieldAccessor;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.PluginSimpleFunction;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.QRT;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;
import com.epam.deltix.util.jcg.JVariable;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *  The superclass of any data value type. QTypes are used to create QValues
 *  and to generate code for operations that do not involve QValues.
 */
public abstract class QType <T extends DataType> {
    public static final int     SIZE_VARIABLE = -1;

    public static QType     forDataType (DataType type) {
        return forDataType(type, false);
    }

    private static QType forDataType(DataType type, boolean typed) {
        Class <?>               tc = type.getClass ();

        if (tc == IntegerDataType.class)
            return (new QIntegerType ((IntegerDataType) type));

        if (tc == BooleanDataType.class)
            return (new QBooleanType ((BooleanDataType) type));

        if (tc == VarcharDataType.class)
            return (new QVarcharType ((VarcharDataType) type));

        if (tc == FloatDataType.class)
            return (new QFloatType ((FloatDataType) type));

        if (tc == DateTimeDataType.class)
            return (new QDateTimeType ((DateTimeDataType) type));

        if (tc == EnumDataType.class)
            return (new QEnumType ((EnumDataType) type));

        if (tc == CharDataType.class)
            return (new QCharType ((CharDataType) type));

        if (tc == TimeOfDayDataType.class)
            return (new QTimeOfDayType ((TimeOfDayDataType) type));

        if (tc == BinaryDataType.class)
            return (new QBinaryType ((BinaryDataType) type));

        if (tc == QueryDataType.class)
            return (new QQueryType ((QueryDataType) type));

        if (tc == ClassDataType.class) {
            ClassDataType cdt = (ClassDataType) type;
            if (cdt.isFixed()) {
                try {
                    return new QExtendedObjectType(cdt);
                } catch (ClassNotFoundException ignored) {
                }
            }
            return new QObjectType(cdt);
        }

        if (tc == ArrayDataType.class) {
            return (new QArrayType((ArrayDataType) type));
        }

        throw new UnsupportedOperationException (tc.getSimpleName ());
    }

    public static QType forExpr(CompiledExpression e) {
        QType type;
        if (e instanceof PluginSimpleFunction) {
            type = forDataType(e.type, true);
        } else {
            type = forDataType(e.type);
        }

        if (e instanceof FieldAccessor) {
            FieldAccessor fs = (FieldAccessor) e;
            if (fs.slicedType != null) {
                type = new QArrayType((ArrayDataType) fs.type, QType.forDataType(fs.slicedType));
            }
        }

        return type;
    }

    public final T      dt;

    protected QType (T dt) {
        this.dt = dt;
    }

    public void                 moveNoNullCheck (
        QValue                      from,
        QValue                      to,
        JCompoundStatement          addTo
    )
    {
        addTo.add (to.write (from.read ()));
    }
    
    public void                 move (
        QValue                      from,
        QValue                      to,
        JCompoundStatement          addTo
    )
    {
        if (from.type.isNullable () && !to.type.isNullable ())
            addTo.add (
                CTXT.ifStmt (
                    from.readIsNull (true),
                    QCGHelpers.throwNVX ()
                )
            );

        moveNoNullCheck (from, to, addTo);
    }

    public JStatement           skip (JExpr input) {
        throw new UnsupportedOperationException (
            getEncodedFixedSize () == SIZE_VARIABLE ?
                "Unimplemented for " + getClass().getSimpleName() :
                "Fixed size - should have been skipped by #bytes"            
        );
    }

    /**
     *  Returns whether the underlying value is nullable.
     */
    public final boolean        isNullable () {
        return dt.isNullable ();
    }

    /**
     *  Returns the size, if fixed, or SIZE_VARIABLE if the size is variable.
     */
    public abstract int         getEncodedFixedSize ();

    /**
     *  Generates decoding code.
     * 
     *  @param input    MemoryDataInput instance
     *  @param value    Output value.
     *  @return         A single or compound statement.
     */
    public abstract JStatement  decode (
        JExpr                       input,
        QValue                      value
    );

    public abstract void        encode (
        QValue                      value, 
        JExpr                       output,
        JCompoundStatement          addTo
    );
    
    public abstract Class <?>   getJavaClass ();
    
    public boolean              instanceAllocatesMemory () {
        return (false);
    }
    
    public JExpr                checkNull (JExpr e, boolean eq) {
        return (CTXT.binExpr (e, eq ? "==" : "!=", getNullLiteral ()));
    }
    
    /**
     *  Declares a variable in the supplied container.
     * 
     *  @param container    Place to declare a variable.
     *  @param registry     Class registry, in case it's needed.
     *  @param setNull      If the variable should be initialized to NULL.
     * 
     *  @return     A QValue instance representing the variable
     */
    public QValue               declareValue (
        String                      comment,
        QVariableContainer          container, 
        QClassRegistry              registry,
        boolean                     setNull
    )
    {
        JVariable       v = 
            container.addVar (
                comment,
                false, 
                getJavaClass (), 
                setNull ? getNullLiteral () : null
            );
        
        return (new QExprValue (this, container.access (v)));
    }

    /**
     *  Creates a constant value, if supported.
     * 
     *  @param obj  The actual value of the constant. Exact type required
     *              is implementation-dependent.
     *  @return     A QValue instance representing the constant.
     */
    public final QValue         makeConstant (Object obj) {
        return (new QExprValue (this, obj == null ? getNullLiteral () : makeConstantExpr (obj)));
    }

    /**
     *  Creates a constant expression, if supported.
     * 
     *  @param obj  The actual value of the constant. Exact type required
     *              is implementation-dependent. Never null.
     *  @return     A JExpr that can be assigned.
     */
    protected JExpr             makeConstantExpr (Object obj) {
        throw notImplemented ();
    }

    /**
     *  Generate code for writing a null into the data output. This method
     *  asserts that the type is nullable and then calls {@link #encodeNullImpl}.
     * 
     *  @param output       MemoryDataOutput instance.
     *  @param addTo        Compound statement to add code to.
     */
    public final void           encodeNull (
        JExpr                       output,
        JCompoundStatement          addTo
    )
    {
        if (isNullable ())
            encodeNullImpl(output, addTo);
        else
            throw new IllegalStateException ("type is not Nullable " + dt.getBaseName());
    }

    /**
     *  Override this method to do useful work.
     * 
     *  @param output       MemoryDataOutput instance.
     *  @param addTo        Compound statement to add code to.
     */
    protected void              encodeNullImpl (
        JExpr                       output,
        JCompoundStatement          addTo
    ) 
    {
        throw notImplemented ();
    }

    protected UnsupportedOperationException notImplemented () {
        throw new UnsupportedOperationException (
            "Not implemented for " + getClass ().getSimpleName ()
        );
    }
    
    public abstract JExpr       getNullLiteral ();
    
    /**
     *  Compute s, unless arg is null, in which case if out is nullable, set out
     *  to null, otherwise throw NVX.
     * 
     *  @param s        Statement to compute if arg value is not null.
     *  @param arg      The arg value to check for null.
     *  @param out      Set it to null if it's nullable
     * 
     *  @return     Expression with the result.
     */
    public static JStatement    wrapWithNullCheck (
        JStatement                  s,
        QValue                      arg,
        QValue                      out
    )
    {
        if (arg.type.isNullable ()) 
            return (
                CTXT.ifStmt (
                    arg.readIsNull (true),
                    out.type.isNullable () ? 
                        out.writeNull() :
                        QCGHelpers.throwNVX (),
                    s
                )
            );                    

        return (s);
    }

    public static void          genUnOp (
        String                      op,
        QValue                      arg,
        QValue                      out,
        JCompoundStatement          addTo
    )
    {
        JExpr               argExpr = arg.read ();
        JExpr               e;
        
        if (op.startsWith ("QRT."))
            e = CTXT.staticCall (QRT.class, op.substring (4), argExpr);
        else
            throw new RuntimeException (op);
        
        addTo.add (wrapWithNullCheck (out.write (e), arg, out));
    }
    
    public static void          genBinOp (
        QValue                      left,
        String                      op,
        QValue                      right,
        QValue                      out,
        JCompoundStatement          addTo
    )
    {
        JExpr               leftArg = left.read ();
        JExpr               rightArg = right.read ();

        JExpr               e;

        if (op.startsWith ("QRT."))
            e = CTXT.staticCall (QRT.class, op.substring (4), leftArg, rightArg);
        else {
            e = CTXT.binExpr (leftArg, op, rightArg);
            
            if (op.equals ("==") || op.equals ("!=") || op.equals (">") || 
                op.equals ("<") || op.equals (">=") || op.equals ("<="))
                e = CTXT.staticCall (QRT.class, "bpos", e);
        }
        
        JStatement          s = out.write (e);
        
        s = wrapWithNullCheck (s, left, out);
        s = wrapWithNullCheck (s, right, out);

        addTo.add (s);
    }

    public static void          genDecimalBinOp (
            QValue                      left,
            String                      op,
            QValue                      right,
            QValue                      out,
            JCompoundStatement          addTo
    ) {
        JExpr leftArg = left.read();
        JExpr rightArg = right.read();
        JExpr e = CTXT.staticCall(Decimal64Utils.class, op, leftArg, rightArg);
        JStatement s = out.write(e);
        s = wrapWithNullCheck(s, left, out);
        s = wrapWithNullCheck(s, right, out);
        addTo.add(s);
    }

    public static void          genDecimalComparison (
            QValue                      left,
            QValue                      right,
            String                      operator,
            int                         value,
            QValue                      out,
            JCompoundStatement          addTo
    ) {
        JExpr leftArg = left.read();
        JExpr rightArg = right.read();
        JExpr e = CTXT.binExpr(CTXT.staticCall(Decimal64Utils.class, "compareTo", leftArg, rightArg), operator, CTXT.intLiteral(value));
        e = CTXT.staticCall(QRT.class, "bpos", e);
        JStatement s = out.write(e);
        s = wrapWithNullCheck(s, left, out);
        s = wrapWithNullCheck(s, right, out);
        addTo.add(s);
    }

    public static void          genNegate (
            QValue                      arg,
            QValue                      out,
            JCompoundStatement          addTo
    ) {
        JExpr argExpr = arg.read();
        JExpr e = argExpr.negate();
        JStatement s = out.write(e);
        s = wrapWithNullCheck(s, arg, out);
        addTo.add(s);
    }

    public static void          genDecimalNegate (
            QValue                      arg,
            QValue                      out,
            JCompoundStatement          addTo
    ) {
        JExpr argExpr = arg.read();
        JExpr e = CTXT.staticCall(Decimal64Utils.class, "negate", argExpr);
        JStatement s = out.write(e);
        s = wrapWithNullCheck(s, arg, out);
        addTo.add(s);
    }

    public static void          decimalToFloat (
            QValue                      from,
            QValue                      to,
            JCompoundStatement          addTo
    ) {
        if (from.type.isNullable() && !to.type.isNullable())
            addTo.add(CTXT.ifStmt(from.readIsNull(true), QCGHelpers.throwNVX()));

        addTo.add(to.write(CTXT.staticCall(Decimal64Utils.class, "toDouble", from.read())));
    }

    public static void          integerToDecimal (
            QValue                      from,
            QValue                      to,
            JCompoundStatement          addTo
    ) {
        if (from.type.isNullable() && !to.type.isNullable())
            addTo.add(CTXT.ifStmt(from.readIsNull(true), QCGHelpers.throwNVX()));

        addTo.add(to.write(CTXT.staticCall(Decimal64Utils.class, "fromLong", from.read())));
    }

    public static void          negate (
            QValue                      from,
            QValue                      to,
            JCompoundStatement          addTo
    ) {
        if (from.type.isNullable() && !to.type.isNullable())
            addTo.add(CTXT.ifStmt(from.readIsNull(true), QCGHelpers.throwNVX()));

        addTo.add(to.write(from.read().negate()));
    }
    
    public static void          genEqOp (
        QValue                      left,
        String                      op,
        boolean                     positive,
        QValue                      right,
        QValue                      out,
        JCompoundStatement          addTo
    )
    {
        if (!(out.type instanceof QBooleanType))
            throw new IllegalArgumentException (
                "output type is not BOOLEAN: " + out.type
            );
        
        boolean             leftIsNullable = left.type.isNullable ();
        boolean             rightIsNullable = right.type.isNullable ();
                        
        JExpr               leftArg = left.read ();
        JExpr               rightArg = right.read ();

        JExpr               e;
        
        if (op.startsWith ("QRT."))
            e = CTXT.staticCall (QRT.class, op.substring (4), leftArg, rightArg);
        else {
            e = CTXT.binExpr (leftArg, op, rightArg);
            
            if (op.equals ("==") || op.equals ("!=") || op.equals (">") || 
                op.equals ("<") || op.equals (">=") || op.equals ("<="))
                e = CTXT.staticCall (QRT.class, "bpos", e);
        }
        
        JStatement          s = out.write (e);
        JExpr               negative = QBooleanType.getLiteral (!positive);
        
        if (leftIsNullable) {
            if (rightIsNullable) {
                s = 
                    CTXT.ifStmt (
                        left.readIsNull (true),
                        out.write (QBooleanType.cleanToNullable (right.readIsNull (positive))),
                        CTXT.ifStmt (
                            right.readIsNull (true), // but left is not null
                            out.write (negative),
                            s
                        )
                    );
            }
            else {
                s = 
                    CTXT.ifStmt (
                        left.readIsNull (true),
                        out.write (negative),
                        s
                    );
            }
        }
        else if (rightIsNullable) {
            s = 
                CTXT.ifStmt (
                    right.readIsNull (true),
                    out.write (negative),
                    s
                );
        }                

        addTo.add (s);
    }

    public static void          genArrayLen (
            QValue                      arg,
            QValue                      out,
            JCompoundStatement          addTo
    ) {
        JExpr argExpr = arg.read();
        JExpr e = argExpr.call("size");
        JStatement s = out.write(e);
        s = wrapWithNullCheck(s, arg, out);
        addTo.add(s);
    }
}