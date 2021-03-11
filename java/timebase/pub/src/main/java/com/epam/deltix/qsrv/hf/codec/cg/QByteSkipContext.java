package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public final class QByteSkipContext {
    private int                         bytesSkipped = 0;
    private final JExpr                 input;
    final JCompoundStatement            addTo;
    private final String                function; 

    public QByteSkipContext (JExpr input, JCompoundStatement addTo) {
        this(true, input, addTo);
    }

    public QByteSkipContext (boolean isInput, JExpr memoryData, JCompoundStatement addTo) {
        this.input = memoryData;
        this.addTo = addTo;
        this.function = isInput ? "skipBytes": "skip";
    }

    public void                 skipBytes (int n) {
        bytesSkipped += n;
    }

    public void                 flush () {
        if (bytesSkipped == 0)
            return;

        addTo.add (input.call (function, CTXT.intLiteral (bytesSkipped)));
        bytesSkipped = 0;
    }

    public JStatement                 flush2 () {
        if (bytesSkipped == 0)
            return null;

        final JStatement stmt = input.call(function, CTXT.intLiteral(bytesSkipped)).asStmt();
        bytesSkipped = 0;
        return stmt;
    }
}
