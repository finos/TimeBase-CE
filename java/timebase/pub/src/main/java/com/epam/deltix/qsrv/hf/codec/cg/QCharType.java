package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QCharType extends QPrimitiveType <CharDataType> {

    public QCharType(CharDataType dt) {
        super(dt);
    }

    @Override
    public Class<?> getJavaClass() {
        return char.class;
    }

    @Override
    protected JExpr getNullLiteral() {
        return CTXT.staticVarRef(CharDataType.class, "NULL");
    }

    @Override
    public JExpr makeConstantExpr(Object obj) {
        return CTXT.charLiteral((Character) obj);
    }

    @Override
    public int getEncodedFixedSize() {
        return 2;
    }

    @Override
    protected JExpr decodeExpr(JExpr input) {
        return input.call("readChar");
    }

    @Override
    protected void encodeExpr(JExpr output, JExpr value, JCompoundStatement addTo) {
        addTo.add(output.call("writeChar", value));
    }
}
