package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

public class QArrayValue extends QValue {
    private final JExpr variable;

    public QArrayValue (QArrayType type, JExpr variable) {
        super (type);

        this.variable = variable;
    }

    public void         skip (JExpr input, JCompoundStatement addTo) {
    }

    @Override
    public JExpr        read () {
        return (variable);
    }

    @Override
    public JExpr        readIsNull (boolean eq) {
        JExpr   e = variable.call ("isNull");

        return (eq ? e : e.not ());
    }

    @Override
    public JStatement write (JExpr arg) {
        return (variable.call ("set", arg).asStmt ());
    }
}
