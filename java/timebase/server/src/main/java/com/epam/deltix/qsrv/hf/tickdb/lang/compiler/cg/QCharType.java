package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.JStatement;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QCharType extends QType <CharDataType> {

    public QCharType(CharDataType dt) {
        super(dt);
    }

    @Override
    public Class <?>            getJavaClass() {
        return char.class;
    }

    @Override
    public JExpr                getNullLiteral() {
        return CTXT.staticVarRef (CharDataType.class, "NULL");
    }

    @Override
    public JExpr                makeConstantExpr (Object obj) {
        return CTXT.charLiteral ((Character) obj);
    }

    @Override
    public int                  getEncodedFixedSize () {
        return 2;
    }

    @Override
    public JStatement           decode (JExpr input, QValue value) {
        return (value.write (input.call ("readChar")));
    }

    @Override
    public void                 encode (
        QValue                      value, 
        JExpr                       output,
        JCompoundStatement          addTo
    )
    {
        addTo.add (output.call ("writeChar", value.read ()));
    }
}
