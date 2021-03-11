package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public class FieldCodecFactory {

    public static FieldDecoder createDecoder(TypeLoader loader, NonStaticFieldLayout f) {
        DataType        type = f.getType ();

        try {
            final boolean isBound = f.isBound();
            final Class<?> fieldType = isBound ? f.getFieldType() : null;

            if (type instanceof BooleanDataType) {
                return (new BooleanFieldDecoder(f));
            } else if (type instanceof IntegerDataType) {
                IntegerDataType idt = (IntegerDataType) type;

                switch (idt.getSize()) {
                    case IntegerDataType.PACKED_UNSIGNED_INT:
                        return (new PackedUIntFieldDecoder(f));

                    case IntegerDataType.PACKED_UNSIGNED_LONG:
                        return (new PackedULongFieldDecoder(f));

                    case IntegerDataType.PACKED_INTERVAL:
                        return (new IntTimeIntervalFieldDecoder(f));

                    default:
                        return (new FixedSizeIntegerFieldDecoder(f, idt.getSize()));
                }
            } else if (type instanceof FloatDataType) {
                FloatDataType fdt = (FloatDataType) type;

                switch (fdt.getScale()) {
                    case FloatDataType.FIXED_FLOAT:
                        return (new FloatFieldDecoder(f));

                    case FloatDataType.FIXED_DOUBLE:
                        return (new DoubleFieldDecoder(f));

                    case FloatDataType.SCALE_DECIMAL64:
                        return (new DoubleFieldDecoder(f));

                    case FloatDataType.SCALE_AUTO:
                    default:
                        if (isBound  && fieldType == float.class)
                            throw new IllegalArgumentException(fdt.getEncoding() + " encoding is not supported for a float field");
                        else
                            return (new ScaledDoubleFieldDecoder(f));
                }
            } else if (type instanceof VarcharDataType) {

                final VarcharDataType sdt = (VarcharDataType) type;
                switch (sdt.getEncodingType()) {
                    case VarcharDataType.INLINE_VARSIZE:
                        return (new StringFieldDecoder(f));
                    case VarcharDataType.FORWARD_VARSIZE:
                        return (new FwdStringFieldDecoder(f));
                    case VarcharDataType.ALPHANUMERIC:
                        return (new AlphanumericFieldDecoder(f, sdt.getLength()));
                    default:
                        throw new RuntimeException(String.valueOf(sdt.getEncodingType()));
                }
            } else if (type instanceof CharDataType)
                return (new CharFieldDecoder(f));
            else if (type instanceof TimeOfDayDataType)
                return (new TimeOfDayFieldDecoder(f));
            else if (type instanceof DateTimeDataType)
                return (new DateTimeFieldDecoder(f));
            else if (type instanceof EnumDataType)
                return new EnumFieldDecoder(f);
            else if (type instanceof BinaryDataType)
                return (new BinaryFieldDecoder(f, ((BinaryDataType) type).getCompressionLevel()));
            else if (type instanceof ClassDataType)
                return (new ClassFieldDecoder(loader, f));
            else if (type instanceof ArrayDataType) {
                return new ArrayFieldDecoder(loader, f);
            } else
                throw new RuntimeException(type.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    @SuppressWarnings ("unchecked")
//    private static FieldDecoder                 createDecoderForNullable (NonStaticFieldLayout f) throws InstantiationException,
//            IllegalAccessException, InvocationTargetException {
//        final DataType type = f.getType();
//        final Constructor<FieldDecoder> cnst = decodersMap.get(type.getClass().getSimpleName());
//        if (cnst != null)
//            return cnst.newInstance(f);
//        else if (type instanceof IntegerDataType) {
//            final IntegerDataType idt = (IntegerDataType) type;
//            return createDecoderForInteger(f, idt);
//        } else if (type instanceof FloatDataType) {
//            final FloatDataType fdt = (FloatDataType) type;
//
//            //TODO: Alex - decodersArray is always NULL?
//            switch (fdt.getScale()) {
//                case FloatDataType.FIXED_FLOAT:
//                    return (FieldDecoder) decodersArray[FLOAT_FIELD_ID].newInstance(f);
//
//                case FloatDataType.FIXED_DOUBLE:
//                    return (FieldDecoder) decodersArray[DOUBLE_FIELD_ID].newInstance(f);
//
//                case FloatDataType.SCALE_AUTO:
//                default:
//                    return (FieldDecoder) decodersArray[SCALED_DOUBLE_FIELD_ID].newInstance(f);
//            }
//        } else if (type instanceof VarcharDataType)
//            return (FieldDecoder) decodersArray[FIXED_SIZE_INTEGER_ID].newInstance(f, 8);
//        else if (type instanceof EnumDataType)
//            return ((EnumDataType) type).descriptor.isBitmask() ?
//                    (FieldDecoder) decodersArray[BITWISE_ENUM_ID].newInstance(f) :
//                    (FieldDecoder) decodersArray[ENUM_ID].newInstance(f);
//        else
//            throw new RuntimeException(type.toString());
//    }

    public static FieldEncoder createEncoder(TypeLoader loader, NonStaticFieldLayout f) {
        DataType        type = f.getType ();


        if (type instanceof BooleanDataType) {
            return (new BooleanFieldEncoder(f));
        } else if (type instanceof IntegerDataType) {
            IntegerDataType idt = (IntegerDataType) type;


            switch (idt.getSize()) {
                case IntegerDataType.PACKED_UNSIGNED_INT:
                    return (new PackedUIntFieldEncoder(f));

                case IntegerDataType.PACKED_UNSIGNED_LONG:
                    return (new PackedULongFieldEncoder(f));

                case IntegerDataType.PACKED_INTERVAL:
                    return (new IntTimeIntervalFieldEncoder(f));

                default:
                    return (new FixedSizeIntegerFieldEncoder(f, idt.getSize()));
            }
        } else if (type instanceof FloatDataType) {
            FloatDataType fdt = (FloatDataType) type;

            switch (fdt.getScale()) {
                case FloatDataType.FIXED_FLOAT:
                    return (new FloatFieldEncoder(f));

                case FloatDataType.FIXED_DOUBLE:
                    return (new DoubleFieldEncoder(f));

                case FloatDataType.SCALE_DECIMAL64:
                    return (new DoubleFieldEncoder(f));

                case FloatDataType.SCALE_AUTO:
                    return (new ScaledDoubleFieldEncoder(f, -1));

                default:
                    return (new ScaledDoubleFieldEncoder(f, fdt.getScale()));
            }
        } else if (type instanceof VarcharDataType) {
            VarcharDataType sdt = (VarcharDataType) type;
            switch (sdt.getEncodingType()) {
                case VarcharDataType.INLINE_VARSIZE:
                    return (new StringFieldEncoder(f));

                case VarcharDataType.FORWARD_VARSIZE:
                    return (new FwdStringFieldEncoder(f));

                case VarcharDataType.ALPHANUMERIC:
                    return (new AlphanumericFieldEncoder(f, sdt.getLength()));

                default:
                    throw new RuntimeException(String.valueOf(sdt.getEncodingType()));
            }
        } else if (type instanceof CharDataType)
            return (new CharFieldEncoder(f));
        else if (type instanceof TimeOfDayDataType)
            return (new TimeOfDayFieldEncoder(f));
        else if (type instanceof DateTimeDataType)
            return (new DateTimeFieldEncoder(f));
        else if (type instanceof EnumDataType)
            return new EnumFieldEncoder(f);
        else if (type instanceof BinaryDataType) {
            final BinaryDataType bdt = (BinaryDataType) type;
            return (new BinaryFieldEncoder(f, bdt.getMaxSize(), bdt.getCompressionLevel()));
        } else if (type instanceof ClassDataType)
            return (new ClassFieldEncoder(loader, f));
        else if (type instanceof ArrayDataType) {
            return new ArrayFieldEncoder(loader, f);
        } else
            throw new RuntimeException(type.toString());
    }


    static FieldEncoder []          createEncoders (RecordLayout layout) {
        NonStaticFieldLayout []         infos = layout.getNonStaticFields ();
        int                             num = infos == null ? 0 : infos.length;
        FieldEncoder []                 ret = new FieldEncoder [num];

        for (int ii =0; ii < num; ii++)
            ret [ii] = createEncoder (layout.getLoader(), infos [ii]);

        return (ret);
    }

    public static FieldDecoder []          createDecoders (RecordLayout layout) {
        NonStaticFieldLayout []         infos = layout.getNonStaticFields ();
        int                             num = infos == null ? 0 : infos.length;
        FieldDecoder []                 ret = new FieldDecoder [num];

        for (int ii =0; ii < num; ii++)
            ret [ii] = createDecoder (layout.getLoader(), infos [ii]);

        return (ret);
    }

//    private static FieldDecoder createDecoderForInteger(NonStaticFieldLayout f, IntegerDataType idt)
//            throws InstantiationException, IllegalAccessException, InvocationTargetException {
//        switch (idt.getSize()) {
//            case IntegerDataType.PACKED_UNSIGNED_INT:
//                return (FieldDecoder) decodersArray[PACKED_UINT_ID].newInstance(f);
//
//            case IntegerDataType.PACKED_UNSIGNED_LONG:
//                return (FieldDecoder) decodersArray[PACKED_ULONG_ID].newInstance(f);
//
//            case IntegerDataType.PACKED_INTERVAL:
//                return (FieldDecoder) decodersArray[INT_TIME_INTERVAL_ID].newInstance(f);
//
//            default:
//                return (FieldDecoder) decodersArray[FIXED_SIZE_INTEGER_ID].newInstance(f, idt.getSize());
//        }
//    }
}
