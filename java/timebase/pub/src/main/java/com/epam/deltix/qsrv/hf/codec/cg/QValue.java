package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 * Represents a value bound to a constant, variable or object field.
 * <p>
 * Notice that a value can be kept in more then one java variable 
 * (see {@link QAVariable} for example).
 * </p>
 */
public class QValue<T extends QType> {
    public final T type;

    public QValue(T type) {
        this.type = type;
    }

    public JExpr readIsNull(boolean eq) {
        throw new UnsupportedOperationException(
            "Not implemented for  " + getClass().getSimpleName()
        );
    }

    public JStatement writeNull() {
        throw new UnsupportedOperationException(
            "Not implemented for " + getClass().getSimpleName()
        );
    }

    public JStatement  writeIsNull (JExpr arg) {
        throw new UnsupportedOperationException(
            "Not implemented for  " + getClass().getSimpleName()
        );
    }

    public JExpr       read () {
        throw new UnsupportedOperationException(
            "Not implemented for  " + getClass().getSimpleName()
        );
    }

    public JStatement  write (JExpr arg) {
        throw new UnsupportedOperationException(
            "Not implemented for  " + getClass().getSimpleName()
        );
    }

    public void decode(JExpr input, JCompoundStatement addTo) {
        throw notImplemented();
    }

    public void decodeRelative(
        JExpr input,
        QValue base,
        JExpr isBaseNull,
        JCompoundStatement addTo
    )
    {
        throw notImplemented();
    }

    public void encode(JExpr output, JCompoundStatement addTo) {
        throw notImplemented();
    }

    protected UnsupportedOperationException notImplemented() {
        return new UnsupportedOperationException(
            "Not implemented for " + getClass().getSimpleName()
        );
    }
}
