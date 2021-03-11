package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.lang.reflect.InvocationTargetException;
import java.util.AbstractList;
import java.util.NoSuchElementException;

/**
 *
 */
class ArrayFieldEncoder<T> extends FieldEncoder implements ContainerEncoder {
    private final ArraysAdapters.ArrayAdapter<T> arrayAdapter;

    private final EncodingContext       ctx = new EncodingContext(new RecordLayout(new RecordClassDescriptor(null, null, false, null)));

    // unbound API implementation
    private MemoryDataOutput            out;
    private final FieldEncoder          encoder;
    private WritableValueImpl           element;
    private final MemoryDataOutput      local = new MemoryDataOutput();

    private int                         length = -1;
    private int                         index = -1;
    private int                         currentPosition = -1;
    private boolean                     isNull = false;

    public ArrayFieldEncoder(TypeLoader loader, NonStaticFieldLayout f) {
        super(f);

        final DataType elementType = ((ArrayDataType) f.getType()).getElementDataType();
        final NonStaticFieldLayout layout = new NonStaticFieldLayout(f, new NonStaticDataField(f.getName(), "array elementType", elementType));

        this.encoder = FieldCodecFactory.createEncoder(loader, layout);

        if (f.isBound())
            arrayAdapter = ArraysAdapters.createEncodeAdapter(fieldType, encoder, elementType instanceof EnumDataType);
        else
            arrayAdapter = null;

        this.ctx.out = local;
    }

    @Override
    protected void copy(Object obj, EncodingContext ctx) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        final Object value = getter.get(obj);
        if (value == null) {
            writeNull(ctx);
        } else {
            // backup position and skip one byte to later save the field size there
            final int pos = ctx.out.getPosition();
            ctx.out.skip(1);

            // array length
            @SuppressWarnings("unchecked")
            final AbstractList<T> a = (AbstractList<T>) value;
            int len = a.size();
            MessageSizeCodec.write(len, ctx.out);

            for (int i = 0; i < len; i++) {
                arrayAdapter.encode(a, i, ctx);
            }

            // rewind back and store raw message size
            CodecUtils.storeFieldSize(pos, ctx.out);
        }
    }

    @Override
    public void beginWrite(MemoryDataOutput out) {
        this.out = out;
        this.local.reset();
        this.isNull = false;
        this.element = null;

        index = length = currentPosition = -1;
    }

    @Override
    public void endWrite() {

        if (element != null) {
            element.endWrite();
            checkNullElement();
        }

        if (length != -1) {
            int size = local.getSize() + MessageSizeCodec.fieldSize(length);

            MessageSizeCodec.write(size, out); // total container size
            MessageSizeCodec.write(length, out); // array length

            out.write(local.getBuffer(), 0, local.getSize()); // array elements
        } else if (!isNull) {
            writeNull(out);
        }
    }

    @Override
    void writeNull(EncodingContext ctxt) {
        writeNull(ctxt.out);
    }

    private void writeNull(MemoryDataOutput out) {
        if (!isNullable) {
            throwNotNullableException();
        } else {
            MessageSizeCodec.write(ArrayFieldDecoder.NULL_CODE, out);
            isNull = true;
        }
    }

    @Override
    void setString(CharSequence value, EncodingContext ctxt) {
        throw new UnsupportedOperationException("not supported for ARRAY field");
    }

    @Override
    void setArrayLength(int len, EncodingContext ctxt) {

        if (index > len)
            throw new IllegalStateException("Cannot change array size less than current (" + index + ")");

        this.length = len;
    }

    @Override
    WritableValue               nextWritableElement() {
        if (length == -1)
            throw new UnsupportedOperationException("Array length is undefined. setArrayLength() invoke required.");

        if (++index >= length)
            throw new NoSuchElementException("Array boundary exceeded ( > " + length + ")");

        if (element == null) {
            element = new WritableValueImpl(encoder, ctx) { };
        } else {
            element.endWrite();
            checkNullElement();
        }

        currentPosition = local.getPosition();

        element.beginWrite(local);

        return element;
    }

    private void            checkNullElement() {
        // if nothing written - check that field allow nulls
        if (currentPosition == local.getPosition()) {
            if (encoder.isNullable)
                encoder.writeNull(ctx);
            else
                throw new IllegalArgumentException(String.format("'%s' field array element is not nullable", fieldName));
        }
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return getter.get(message) == null;
    }
}
