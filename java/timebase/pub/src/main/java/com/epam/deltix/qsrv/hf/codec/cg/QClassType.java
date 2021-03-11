package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.jcg.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public class QClassType extends QPrimitiveType <ClassDataType> {
    public QClassType(ClassDataType dt) {
        super (dt);
    }

    @Override
    public int          getEncodedFixedSize () {
        return (SIZE_VARIABLE);
    }

    @Override
    public Class<?> getJavaClass() {
        throw new UnsupportedOperationException(
                "Not implemented for " + getClass().getSimpleName()
        );
    }

    @Override
    public void skip(JExpr input, JCompoundStatement addTo) {
        addTo.add (input.call ("skipBytes", CTXT.staticCall(MessageSizeCodec.class, "read", input)));
    }

    @Override
    protected JExpr getNullLiteral() {
        return CTXT.nullLiteral();
    }
}
