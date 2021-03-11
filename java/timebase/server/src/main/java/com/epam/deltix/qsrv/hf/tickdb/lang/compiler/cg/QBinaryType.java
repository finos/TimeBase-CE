package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.Binary;
import com.epam.deltix.qsrv.hf.pub.md.BinaryDataType;
import com.epam.deltix.qsrv.hf.codec.BinaryCodec;
import com.epam.deltix.util.jcg.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QBinaryType extends QType <BinaryDataType> {
    public QBinaryType (BinaryDataType dt) {
        super(dt);
    }
    
    @Override
    public QValue               declareValue (
        String                      comment,
        QVariableContainer          container, 
        QClassRegistry              registry,
        boolean                     setNull
    )
    {
        JExpr           init =
            CTXT.newExpr (Binary.class, CTXT.intLiteral (dt.getCompressionLevel ()));
        
        JVariable       v = container.addVar (comment, true, Binary.class, init);
        
        return (new QBinaryValue (this, container.access (v)));
    }
    
    @Override
    public void             moveNoNullCheck (
        QValue                  from,
        QValue                  to,
        JCompoundStatement      addTo
    )
    {
        addTo.add (to.read ().call ("set", from.read ()));
    }

    @Override
    public int              getEncodedFixedSize() {
        return SIZE_VARIABLE;
    }

    @Override
    public boolean          instanceAllocatesMemory () {
        return (true);
    }
    
    @Override
    public Class <?>        getJavaClass() {
        throw new UnsupportedOperationException (
            "Not implemented for " + getClass ().getSimpleName ()
        );
    }

    @Override
    public JStatement       skip(JExpr input) {
        return (CTXT.staticCall (BinaryCodec.class, "skip", input).asStmt ());
    }

    @Override
    public JExpr            getNullLiteral() {
        return CTXT.nullLiteral ();
    }

    @Override
    protected void          encodeNullImpl (JExpr output, JCompoundStatement addTo) {
        addTo.add (CTXT.staticCall (BinaryCodec.class, "writeNull", output));
    }

    @Override
    public JStatement       decode (JExpr input, QValue value) {
        return (value.read ().call ("decode", input).asStmt ());
    }

    @Override
    public void             encode (QValue value, JExpr output, JCompoundStatement addTo) {
        addTo.add (value.read ().call ("encode", output));
    }        
}
