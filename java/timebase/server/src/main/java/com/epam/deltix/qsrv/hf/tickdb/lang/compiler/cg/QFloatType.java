package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.util.jcg.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public final class QFloatType extends QNumericType <FloatDataType> {
    static final int KIND_FLOAT = 4;
    static final int KIND_DOUBLE = 8;

    private static int getKind (FloatDataType dt) {
        return (dt.isFloat () ? KIND_FLOAT : KIND_DOUBLE);
    }

    public QFloatType (FloatDataType dt) {
        super (dt, getKind (dt), dt.getMinNotNull (), dt.getMaxNotNull ());
    }

    public boolean              isFloat () {
        return (kind == KIND_FLOAT);
    }

    @Override
    public Class <?>            getJavaClass () {
        switch (kind) {
            case KIND_FLOAT:    return (float.class);
            case KIND_DOUBLE:   return (double.class);
            default:            throw new RuntimeException ("kind = " + kind);
        }
    }

    @Override
    public JExpr                getLiteral (Number value) {
        switch (kind) {
            case KIND_FLOAT:    return (CTXT.floatLiteral (value.floatValue ()));
            case KIND_DOUBLE:   return (CTXT.doubleLiteral (value.doubleValue ()));
            default:            throw new RuntimeException ("kind = " + kind);
        }
    }

    @Override
    public JExpr                getNullLiteral () {
        switch (kind) {
            case KIND_FLOAT:    return (CTXT.staticVarRef (Float.class, "NaN"));
            case KIND_DOUBLE:   return (CTXT.staticVarRef (Double.class, "NaN"));
            default:            throw new RuntimeException ("kind = " + kind);
        }
    }

    @Override
    public JExpr                checkNull (JExpr e, boolean eq) {
        JExpr       isNull;

        switch (kind) {
            case KIND_FLOAT:
                isNull = CTXT.staticCall (Float.class, "isNaN", e);
                break;
                
            case KIND_DOUBLE:
                isNull = CTXT.staticCall (Double.class, "isNaN", e);
                break;

            default:
                throw new RuntimeException ("kind = " + kind);
        }

        return (eq ? isNull : isNull.not ());
    }

    @Override
    public int                  getEncodedFixedSize () {
        switch (dt.getScale ()) {
            case FloatDataType.FIXED_FLOAT:
                return (4);

            case FloatDataType.FIXED_DOUBLE:
                return (8);

            default:
                return (SIZE_VARIABLE);
        }
    }

    @Override
    public JStatement       skip (JExpr input) {
        final String function;

        switch (dt.getScale ()) {
            case FloatDataType.FIXED_FLOAT:
            case FloatDataType.FIXED_DOUBLE:
            case FloatDataType.SCALE_DECIMAL64:
                function = "readLong";
                break;

            default:
                function = "readScaledDouble";
                break;
        }

        return (input.call (function).asStmt ());
    }

    @Override
    protected void          encodeNullImpl(JExpr output, JCompoundStatement addTo) {
        final JExpr     writeExpr;
        final int       scale = dt.getScale();
        final JExpr     value = getNullLiteral ();

        switch (scale) {
            case FloatDataType.FIXED_FLOAT:
                writeExpr = output.call ("writeFloat", value);
                break;

            case FloatDataType.FIXED_DOUBLE:
                writeExpr = output.call ("writeDouble", value);
                break;

            case FloatDataType.SCALE_AUTO:
                writeExpr = output.call ("writeScaledDouble", value);
                break;

            case FloatDataType.SCALE_DECIMAL64:
                writeExpr = output.call ("writeDecimal64", value);
                break;

            default:
                writeExpr = output.call ("writeScaledDouble", value, CTXT.intLiteral (scale));
                break;
        }

        addTo.add (writeExpr);
    }

    @Override
    public JStatement           decode (JExpr input, QValue value) {
        final String function;
        switch (dt.getScale ()) {
            case FloatDataType.FIXED_FLOAT:
                function = "readFloat";
                break;

            case FloatDataType.FIXED_DOUBLE:
                function = "readDouble";
                break;

            case FloatDataType.SCALE_DECIMAL64:
                function = "readDecimal64";
                break;

            default:
                function = "readScaledDouble";
                break;
        }

        return (value.write (input.call (function)));
    }

    @Override
    public void                 encode (
        QValue                      value, 
        JExpr                       output,
        JCompoundStatement          addTo
    )
    {
        JExpr                   e = value.read ();
        final JExpr             writeExpr;
        final int               scale = dt.getScale();

        switch (scale) {
            case FloatDataType.FIXED_FLOAT:
                writeExpr = output.call ("writeFloat", e);
                break;

            case FloatDataType.FIXED_DOUBLE:
                writeExpr = output.call ("writeDouble", e);
                break;

            case FloatDataType.SCALE_AUTO:
                writeExpr = output.call ("writeScaledDouble", e);
                break;

            case FloatDataType.SCALE_DECIMAL64:
                writeExpr = output.call ("writeDecimal64", e);
                break;

            default:
                writeExpr = output.call ("writeScaledDouble", e, CTXT.intLiteral (scale));
                break;
        }

        addTo.add (writeExpr);
    }   
}
