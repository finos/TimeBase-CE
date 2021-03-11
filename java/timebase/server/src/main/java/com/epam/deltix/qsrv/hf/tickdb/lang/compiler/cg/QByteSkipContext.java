package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public final class QByteSkipContext {
    private int                         bytesSkipped = 0;
    private final JExpr                 input;
    final JCompoundStatement            addTo;
    
    public QByteSkipContext (JExpr input, JCompoundStatement addTo) {
        this.input = input;
        this.addTo = addTo;
    }

    public void                 skipBytes (int n) {
        bytesSkipped += n;
    }

    public void                 flush () {
        if (bytesSkipped == 0)
            return;

        addTo.add (input.call ("skipBytesUpTo", CTXT.intLiteral (bytesSkipped)));
        bytesSkipped = 0;
    }   
}
