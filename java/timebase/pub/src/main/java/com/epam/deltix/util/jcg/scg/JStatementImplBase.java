package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;

/**
 *
 */
abstract class JStatementImplBase implements JStatement, JCompStmtElem {
    final JContextImpl                  context;
    
    public JStatementImplBase (JContextImpl context) {
        this.context = context;
    }
}
