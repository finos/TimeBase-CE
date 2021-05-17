/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.pub.codec.TimeIntervalCodec;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
public final class QIntegerType extends QNumericType <IntegerDataType> {
    static final int     KIND_BYTE = 1;
    static final int     KIND_SHORT = 2;
    static final int     KIND_INT = 4;
    static final int     KIND_LONG = 8;

    public final long       nullValue;

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
    public void                 skip (JExpr input, JCompoundStatement addTo) {
        // TODO: optimize skip for packed ints!
        switch (dt.getSize()) {
            case IntegerDataType.PACKED_UNSIGNED_INT:
                addTo.add (input.call ("readPackedUnsignedInt"));
                break;

            case IntegerDataType.PACKED_UNSIGNED_LONG:
                addTo.add (input.call ("readPackedUnsignedLong"));
                break;
                
            case IntegerDataType.PACKED_INTERVAL:
                addTo.add (CTXT.staticCall (TimeIntervalCodec.class, "read", input));
                break;

            default:
                super.skip (input, addTo);   // throw error
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
    protected JExpr decodeExpr(JExpr input) {
        switch (dt.getSize()) {
            case 1:
                return input.call("readByte");

            case 2:
                return input.call("readShort");

            case 4:
                return input.call("readInt");

            case 6:
                return input.call("readLong48");

            case 8:
                return input.call("readLong");

            case IntegerDataType.PACKED_UNSIGNED_INT:
                return CTXT.staticCall(CodecUtils.class, "readPackedUnsignedInt", input);

            case IntegerDataType.PACKED_UNSIGNED_LONG:
                return CTXT.staticCall(CodecUtils.class, "readPackedUnsignedLong", input);

            case IntegerDataType.PACKED_INTERVAL:
                return CTXT.staticCall(TimeIntervalCodec.class, "read", input);

            default:
                throw new UnsupportedOperationException(dt.getEncoding());
        }
    }

    @Override
    protected void encodeExpr(JExpr output, JExpr value, JCompoundStatement addTo) {
        JExpr               writeExpr;

        switch (dt.getSize()) {
            case 1:
                writeExpr = output.call ("writeByte", value);
                break;

            case 2:
                writeExpr = output.call ("writeShort", value);
                break;

            case 4:
                writeExpr = output.call ("writeInt", value);
                break;

            case 6:
                writeExpr = output.call ("writeLong48", value);
                break;

            case 8:
                writeExpr = output.call ("writeLong", value);
                break;

            case IntegerDataType.PACKED_UNSIGNED_INT:
                writeExpr = CTXT.staticCall (CodecUtils.class, "writePackedUnsignedInt", value, output);
                    output.call ("writePackedUnsignedInt", value);
                break;

            case IntegerDataType.PACKED_UNSIGNED_LONG:
                writeExpr = CTXT.staticCall (CodecUtils.class, "writePackedUnsignedLong", value, output);
                break;

            case IntegerDataType.PACKED_INTERVAL:
                writeExpr = CTXT.staticCall (TimeIntervalCodec.class, "write", value, output);
                break;

            default:
                throw new UnsupportedOperationException (dt.getEncoding ());
        }

        addTo.add (writeExpr);
    }

    @Override
    protected Number getMin() {
        return dt.min;
    }

    @Override
    protected Number getMax() {
        return dt.max;
    }
}
