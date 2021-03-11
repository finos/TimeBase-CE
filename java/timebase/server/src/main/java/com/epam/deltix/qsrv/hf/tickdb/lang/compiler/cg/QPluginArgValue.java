package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 *
 */
public class QPluginArgValue extends QValue {
    private final JExpr         instance;
    private final int           argIdx;

    public QPluginArgValue (QType type, JExpr instance, int argIdx) {
        super (type);
        this.instance = instance;
        this.argIdx = argIdx;
    }

    @Override
    public JExpr                read () {
        throw new UnsupportedOperationException ("Can't read plugin args");
    }

    @Override
    public JStatement           write (JExpr arg) {
        return (instance.call ("set" + (argIdx + 1), arg).asStmt ());
    }         
}
