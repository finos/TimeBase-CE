package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 *
 */
public class QPluginResultValue extends QValue {
    private final JExpr         instance;
    
    public QPluginResultValue (QType type, JExpr instance) {
        super (type);
        this.instance = instance;
    }

    @Override
    public JExpr                read () {
        return (instance.call ("get"));        
    }

    @Override
    public JStatement           write (JExpr arg) {
        throw new UnsupportedOperationException ("Can't write plugin result");
    }         
}
