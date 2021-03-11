package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.util.jcg.*;

/**
 *
 */
public abstract class QNumericType <T extends DataType> 
    extends QPrimitiveType <T>
{
    public final int            kind;
    public final Number         min;
    public final Number         max;

    protected QNumericType (T dt, int kind, Number min, Number max) {
        super (dt);
        this.kind = kind;
        this.min = min;
        this.max = max;
    }

    public abstract JExpr       getLiteral (Number value);

    @Override
    public JExpr makeConstantExpr(Object obj) {
        return obj == null ? getNullLiteral() : getLiteral((Number)obj);
    }

    protected boolean hasConstraint() {
        return getMin() != null || getMax() != null;
    }

    protected abstract Number getMin();

    protected abstract Number getMax();
}
