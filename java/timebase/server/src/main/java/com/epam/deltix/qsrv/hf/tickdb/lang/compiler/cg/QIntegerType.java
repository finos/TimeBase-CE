package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.pub.codec.TimeIntervalCodec;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.MdUtil;
import com.epam.deltix.util.jcg.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public final class QIntegerType extends QNumericType <IntegerDataType> {
    static final int     KIND_BYTE = 1;
    static final int     KIND_SHORT = 2;
    static final int     KIND_INT = 4;
    static final int     KIND_LONG = 8;

    public final long       nullValue;

    public static String    getEncodeMethod (int size) {
        switch (size) {
            case 1: return "writeByte";
            case 2: return "writeShort";
            case 4: return "writeInt";
            case 8: return "writeLong";
            default: throw new IllegalStateException("unexpected size " + size);
        }
    }
    
    public static String    getDecodeMethod (int size) {
        switch (size) {
            case 1: return "readByte";
            case 2: return "readShort";
            case 4: return "readInt";
            case 8: return "readLong";
            default: throw new IllegalStateException("unexpected size " + size);
        }
    }
        
    
    private static int      getKind (IntegerDataType dt) {
        int     size = dt.getSize ();

        switch (size) {
            case 1:
                return (KIND_BYTE);

            case 2:
                return (KIND_SHORT);

            case IntegerDataType.PACKED_UNSIGNED_INT:
            case IntegerDataType.PACKED_INTERVAL:
            case 4:
                return (KIND_INT);

            case IntegerDataType.PACKED_UNSIGNED_LONG:
            case 6:
            case 8:
                return (KIND_LONG);

            default:
                throw new RuntimeException ("integer.size = " + size);
        }
    }

    public QIntegerType (IntegerDataType dt) {
        super (dt, getKind (dt), dt.getMinNotNull (), dt.getMaxNotNull ());
        this.nullValue = dt.getNullValue ();
    }

    @Override
    public Class <?>            getJavaClass () {
        switch (kind) {
            case KIND_BYTE:     return (byte.class);
            case KIND_SHORT:    return (short.class);
            case KIND_INT:      return (int.class);
            case KIND_LONG:     return (long.class);
            default:            throw new RuntimeException ("kind = " + kind);
        }
    }

    @Override
    public JExpr                getLiteral (Number value) {
        switch (kind) {
            case KIND_BYTE:     return (CTXT.intLiteral (value.intValue ()).cast (byte.class));
            case KIND_SHORT:    return (CTXT.intLiteral (value.intValue ()).cast (short.class));
            case KIND_INT:      return (CTXT.intLiteral (value.intValue ()));
            case KIND_LONG:     return (CTXT.longLiteral (value.longValue ()));
            default:            throw new RuntimeException ("kind = " + kind);
        }
    }

    @Override
    public JExpr                getNullLiteral () {
        if (IntegerDataType.ENCODING_INT8.equals(dt.getEncoding()))
            return CTXT.staticVarRef(IntegerDataType.class, "INT8_NULL");
        else
            return (getLiteral(nullValue));
    }

    @Override
    public int                  getEncodedFixedSize () {
        int     size = dt.getSize ();

        if (size <= 8)
            return (size);

        return (SIZE_VARIABLE);
    }

    @Override
    public JStatement           skip (JExpr input) {
        // TODO: optimize skip for packed ints!
        switch (dt.getSize()) {
            case IntegerDataType.PACKED_UNSIGNED_INT:
                return (input.call ("readPackedUnsignedInt").asStmt ());
                
            case IntegerDataType.PACKED_UNSIGNED_LONG:
                return (input.call ("readPackedUnsignedLong").asStmt ());
                
            case IntegerDataType.PACKED_INTERVAL:
                return (CTXT.staticCall (TimeIntervalCodec.class, "read", input).asStmt ());                

            default:
                throw new UnsupportedOperationException (
                    "Unimplemented: " + dt.getSize()
                );
        }
    }

    @Override
    protected void encodeNullImpl(JExpr output, JCompoundStatement addTo) {
        final JExpr writeExpr;
        final JExpr value = getNullLiteral();

        switch (dt.getSize()) {
            case 1:
                writeExpr = output.call("writeByte", value);
                break;

            case 2:
                writeExpr = output.call("writeShort", value);
                break;

            case 4:
                writeExpr = output.call("writeInt", value);
                break;

            case 6:
                writeExpr = output.call("writeLong48", value);
                break;

            case 8:
                writeExpr = output.call("writeLong", value);
                break;

            case IntegerDataType.PACKED_UNSIGNED_INT:
                writeExpr = CTXT.staticCall(CodecUtils.class, "writePackedUnsignedInt", value, output);
                output.call("writePackedUnsignedInt", value);
                break;

            case IntegerDataType.PACKED_UNSIGNED_LONG:
                writeExpr = CTXT.staticCall(CodecUtils.class, "writePackedUnsignedLong", value, output);
                break;

            case IntegerDataType.PACKED_INTERVAL:
                writeExpr = CTXT.staticCall(TimeIntervalCodec.class, "write", value, output);
                break;

            default:
                throw new UnsupportedOperationException(dt.getEncoding());
        }

        addTo.add(writeExpr);
    }
       
    @Override
    public JStatement           decode (JExpr input, QValue value) {
        JExpr   e;
        
        switch (dt.getSize()) {
            case 1:
                e = input.call("readByte");
                break;

            case 2:
                e =  input.call("readShort");
                break;

            case 4:
                e =  input.call("readInt");
                break;

            case 6:
                e =  input.call("readLong48");
                break;

            case 8:
                e =  input.call("readLong");
                break;

            case IntegerDataType.PACKED_UNSIGNED_INT:
                e =  CTXT.staticCall (CodecUtils.class, "readPackedUnsignedInt", input);
                break;

            case IntegerDataType.PACKED_UNSIGNED_LONG:
                e =  CTXT.staticCall (CodecUtils.class, "readPackedUnsignedLong", input);
                break;

            case IntegerDataType.PACKED_INTERVAL:
                e =  CTXT.staticCall (TimeIntervalCodec.class, "read", input);
                break;

            default:
                throw new UnsupportedOperationException(dt.getEncoding());
        }
        
        return (value.write (e));
    }

    @Override
    public void                 encode (
        QValue                      value, 
        JExpr                       output,
        JCompoundStatement          addTo
    )
    {
        JExpr               e = value.read ();
        JExpr               writeExpr;

        switch (dt.getSize()) {
            case 1:
                writeExpr = output.call ("writeByte", e);
                break;

            case 2:
                writeExpr = output.call ("writeShort", e);
                break;

            case 4:
                writeExpr = output.call ("writeInt", e);
                break;

            case 6:
                writeExpr = output.call ("writeLong48", e);
                break;

            case 8:
                writeExpr = output.call ("writeLong", e);
                break;

            case IntegerDataType.PACKED_UNSIGNED_INT:
                writeExpr = CTXT.staticCall (CodecUtils.class, "writePackedUnsignedInt", e, output);
                break;

            case IntegerDataType.PACKED_UNSIGNED_LONG:
                writeExpr = CTXT.staticCall (CodecUtils.class, "writePackedUnsignedLong", e, output);
                break;

            case IntegerDataType.PACKED_INTERVAL:
                writeExpr = CTXT.staticCall (TimeIntervalCodec.class, "write", e, output);
                break;

            default:
                throw new UnsupportedOperationException (dt.getEncoding ());
        }

        addTo.add (writeExpr);
    }
}
