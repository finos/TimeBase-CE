package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.NestedObjectCodec;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.IllegalNullValue;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.ValidationError;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class ClassFieldDecoder extends FieldDecoder {
    public final static int NULL_CODE = NestedObjectCodec.NULL_CODE;

    private final FixedBoundDecoderImpl[]   boundDecoders;
    private final FixedUnboundDecoderImpl[] unboundDecoders;

    private final RecordClassDescriptor[]   types;
    private final TypeLoader                typeLoader;

    private MemoryDataInput in;

    ClassFieldDecoder(TypeLoader loader, NonStaticFieldLayout f) {
        super(f);

        this.types = ((ClassDataType) f.getType()).getDescriptors();
        this.typeLoader = loader;

        final int num = types.length;
        if (num > NULL_CODE)
            throw new IllegalArgumentException(
                "Too many classes: " + types.length + " (max 255)"
            );

        if (loader != null) {
            boundDecoders = new FixedBoundDecoderImpl[types.length];
//            for (int ii = 0; ii < types.length; ii++) {
//                final RecordLayout layout = new RecordLayout(loader, types[ii]);
//                boundDecoders[ii] = new FixedBoundDecoderImpl(layout);
//            }
            unboundDecoders = null;
        } else {
            unboundDecoders = new FixedUnboundDecoderImpl[types.length];
//            for (int ii = 0; ii < types.length; ii++) {
//                final RecordLayout layout = new RecordLayout(null, types[ii]);
//                unboundDecoders[ii] = new FixedUnboundDecoderImpl(layout);
//            }
            boundDecoders = null;
        }
    }

    private FixedBoundDecoderImpl getBoundDecoder(int code) {
        if (typeLoader != null) {
            FixedBoundDecoderImpl decoder = boundDecoders[code];
            if (decoder == null)
                decoder = boundDecoders[code] = new FixedBoundDecoderImpl(new RecordLayout(typeLoader, types[code]));

            return decoder;
        }

        return null;
    }

    private FixedUnboundDecoderImpl getUnboundDecoder(int code) {
        FixedUnboundDecoderImpl decoder = unboundDecoders[code];
        if (decoder == null)
            decoder = unboundDecoders[code] = new FixedUnboundDecoderImpl(new RecordLayout(null, types[code]));

        return decoder;
    }

    @Override
    void            skip(DecodingContext ctxt) {
        skipField(ctxt);
    }

    private int     skipField(DecodingContext ctxt) {
        // field size
        int size = MessageSizeCodec.read(ctxt.in);
        int available = ctxt.in.getAvail();

        // we can have incomplete message
        int min = Math.min(size, available);
        ctxt.in.skipBytes(min);
        return min;
    }

    @Override
    protected void          copy(DecodingContext ctxt, Object obj) 
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        // size
        final int size = MessageSizeCodec.read(ctxt.in);
        if (size == 0)
            setter.set(obj, null);
        else {
            final int code = ctxt.in.readUnsignedByte();

            // trick MemoryDataInput to limit available bytes
            FixedBoundDecoderImpl decoder = getBoundDecoder(code);

            if (size - 1 < ctxt.in.getAvail()) {
                final MemoryDataInput in = ctxt.in;
                final int avail = in.getAvail();
                final int pos = in.getCurrentOffset();
                in.setBytes(in.getBytes(), pos, size - 1);

                final Object value = ctxt.manager.useObject(decoder.getTargetClass());
                decoder.decode(ctxt, value);
                setter.set(obj, value);

                // recover backed up length
                in.setBytes(in.getBytes(), in.getCurrentOffset(), avail - (in.getCurrentOffset() - pos));
            } else {
                // we reuse the object, which instantiated in decoder already
                final Object value = ctxt.manager.useObject(decoder.getTargetClass());
                decoder.decode(ctxt, value);
                setter.set(obj, value);
            }
        }
    }

    public Object readObject(DecodingContext ctx) {

        // size
        final int size = MessageSizeCodec.read(ctx.in);

        if (size > 0) {
            final int code = ctx.in.readUnsignedByte();
            FixedBoundDecoderImpl decoder = getBoundDecoder(code);

            final Object value = ctx.manager.useObject(decoder.getTargetClass());

            // trick MemoryDataInput to limit available bytes
            if (size - 1 < ctx.in.getAvail()) {
                final MemoryDataInput in = ctx.in;
                final int avail = in.getAvail();
                final int pos = in.getCurrentOffset();
                in.setBytes(in.getBytes(), pos, size - 1);

                decoder.decode(ctx, value);

                // recover backed up length
                in.setBytes(in.getBytes(), in.getCurrentOffset(), avail - (in.getCurrentOffset() - pos));
            } else {
                decoder.decode(ctx, value);
            }

            return value;
        }

        return null;
    }

    @Override
    protected void          setNull(Object obj) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        setter.set(obj, null);
    }

    @Override
    public UnboundDecoder   getFieldDecoder() throws NullValueException {
        final int size = MessageSizeCodec.read(in);
        if (size == 0)
            throw NullValueException.INSTANCE;
        else {
            int bodyLimit = size + in.getCurrentOffset();

            final int code = in.readUnsignedByte();
            if (code == NULL_CODE)
                throw NullValueException.INSTANCE;
            else {
                FixedUnboundDecoderImpl decoder = getUnboundDecoder(code);

                decoder.beginRead(in);
                decoder.bodyLimit = bodyLimit;
                return decoder;
            }
        }
    }

    @Override
    String getString(DecodingContext ctxt) {
        int i = skipField(ctxt);
        return i > 0 ? "OBJECT" : null;
    }

    void reset(MemoryDataInput in) {
        this.in = in;
    }

    @Override
    int compare(DecodingContext ctxt1, DecodingContext ctxt2) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean isNull(DecodingContext ctxt) {
        return MessageSizeCodec.read(ctxt.in) == 0;
    }

//    public Class[] getPolyClasses() {
//        final Class[] classes = new Class[boundDecoders.length];
//        for (int i = 0; i < boundDecoders.length; i++)
//            classes[i] = boundDecoders[i].getTargetClass();
//
//        return classes;
//    }

    @Override
    public ValidationError validate (DecodingContext ctxt) {
        if (isNullable)
            skip (ctxt);
        else {
            this.in = ctxt.in;
            final int size0 = MessageSizeCodec.read(in);    // read object size

            if (size0 == 0)
                return (new IllegalNullValue(in.getCurrentOffset(), fieldInfo));
            else {
                if (size0 < in.getAvail ())
                {
                    final int limit = deltix.qsrv.hf.codec.CodecUtils.limitMDI (size0, in);     // set limit for reading this object

                    int code = (types.length > 1) ? in.readUnsignedByte() : 0;        // read object code if poly object
                    UnboundDecoder udec = getUnboundDecoder(code);
                    udec.beginRead(in);

                    ValidationError error = udec.validate();    // validate

                    in.setLimit (limit);    // return previous limit
                    return error;
                }
                else
                {
                    int code = (types.length > 1) ? in.readUnsignedByte() : 0;
                    UnboundDecoder udec = getUnboundDecoder(code);
                    udec.beginRead(in);
                    return udec.validate();
                }
            }
        }
        return (null);
    }
}