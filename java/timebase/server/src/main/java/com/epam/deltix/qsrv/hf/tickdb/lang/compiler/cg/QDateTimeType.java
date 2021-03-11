package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.jcg.*;

import java.util.Date;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public class QDateTimeType extends QType <DateTimeDataType> {
    public static final QDateTimeType       NON_NULLABLE =
        new QDateTimeType (new DateTimeDataType (false));

    public static final QDateTimeType       NULLABLE =
        new QDateTimeType (new DateTimeDataType (true));

    public static final JExpr               NULL =
        CTXT.longLiteral (DateTimeDataType.NULL);

    protected QDateTimeType (DateTimeDataType dt) {
        super (dt);
    }

    @Override
    public Class <?>            getJavaClass () {
        return (long.class);
    }

    @Override
    protected JExpr             makeConstantExpr (Object obj) {
        return (CTXT.longLiteral ((Long) obj));
    }

    @Override
    public JExpr                getNullLiteral() {
        return NULL;
    }

    @Override
    public int                  getEncodedFixedSize () {
        return (8);
    }

    @Override
    public JStatement           decode (JExpr input, QValue value) {
        return (value.write (input.call ("readLong")));
    }

    @Override
    public void                 encode (QValue value, JExpr output, JCompoundStatement addTo) {
        addTo.add (output.call ("writeLong", value.read ()));
    }
}
