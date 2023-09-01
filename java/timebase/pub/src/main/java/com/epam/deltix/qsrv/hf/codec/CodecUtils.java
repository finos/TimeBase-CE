/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.RecordClassInfo;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.intp.PolyBoundEncoderImpl;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.FwdStringCodec;
import com.epam.deltix.util.collections.CharSequenceToIntegerMap;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import com.epam.deltix.util.lang.NotFoundException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * User: BazylevD
 * Date: Dec 5, 2008
 * Time: 9:31:59 PM
 */
public abstract class CodecUtils {

    public static int readPackedUnsignedInt(MemoryDataInput in) {
        int ret = in.readPackedUnsignedInt();
        // make back shift
        return (ret == 0) ? IntegerDataType.PUINT30_NULL : ret - 1;
    }

    public static void writePackedUnsignedInt(int v, MemoryDataOutput out) {
        // make shift to store NULL in 1 byte
        v = (v == IntegerDataType.PUINT30_NULL) ? 0 : v + 1;
        out.writePackedUnsignedInt(v);
    }

    public static long readPackedUnsignedLong(MemoryDataInput in) {
        long ret = in.readPackedUnsignedLong();
        // make back shift
        return (ret == 0) ? IntegerDataType.PUINT61_NULL : ret - 1;
    }

    public static void writePackedUnsignedLong(long v, MemoryDataOutput out) {
        // make shift to store NULL in 1 byte
        v = (v == IntegerDataType.PUINT61_NULL) ? 0 : v + 1;
        out.writePackedUnsignedLong(v);
    }

    public static String getString(CharSequence value) {
        return value != null ? value.toString() : null;
    }

    public static void setString(
            CharSequence value,
            MemoryDataOutput out,
            boolean inlineEncoding
    ) {
        if (inlineEncoding)
            out.writeString(value);
        else
            FwdStringCodec.write(value, out);
    }

    // used in QBEnumType
    public static int getBitmaskValue(CharSequenceToIntegerMap symbolToValueMap, CharSequence value) {
        int n = symbolToValueMap.get(value, -1);
        
        if (n >= 0)
            return (n);
                
        int lv = 0;
        final String[] ss = value.toString ().split("\\|");
        
        for (String s : ss) {
            lv |= symbolToValueMap.get(s, -1);
        }
        return lv;        
    }

    // return enum values taking into account ECD mapping
    public static Object[] getEnumValues(Class<?> enumClass, EnumClassDescriptor ecd) {
        // if not enum, skip validation
        if (Util.indexOf(ENUM_ALLOWED_CLASSES, enumClass) != -1) {
            return null;
        } else if (enumClass.isEnum()) {
            final Object[] constants = enumClass.getEnumConstants();
            if (constants.length < ecd.getValues().length)
                throw new IllegalStateException(enumClass.getName() + " is not corresponded to EnumClassDescriptor mapping");
            else {
                final Enum[] javaConstants = new Enum[ecd.getValues().length];

                for (EnumValue enumValue : ecd.getValues()) {
                    @SuppressWarnings("unchecked")
                    final Enum v = Enum.valueOf((Class<Enum>) enumClass, enumValue.symbol);
                    javaConstants[(int) enumValue.value] = v;
                }

                return javaConstants;
            }
        } else {
            return null;
        }
    }

    private static long      getLongFromEnumConstant (Field f) {
        try {
            if (isJavaFlagsEnumConstant (f)) {
                Class <?>   ec = f.getDeclaringClass ();


                    Field       valueField = ec.getDeclaredField ("Value");

                    valueField.setAccessible (true);

                    return (valueField.getLong (f.get (null)));

            }
            else
                return (f.getLong (null));
        } catch (IllegalAccessException x) {
            throw new RuntimeException (x);
        } catch (NoSuchFieldException x) {
            throw new RuntimeException (x);
        }
    }

    private static final Class<?>[] ENUM_ALLOWED_CLASSES =
            new Class<?>[]{
                    byte.class, short.class, int.class, long.class, CharSequence.class, String.class,
            };

    // returns map of the original ordinals to the current. returns null if mapping is not necessary  
    public static int[] getEnumMap(Class<?> enumClass, EnumClassDescriptor ecd) {
        // if not enum, skip validation
        if (Util.indexOf(ENUM_ALLOWED_CLASSES, enumClass) != -1) {
            return null;
        }
        // if Java enum
        else if (enumClass.isEnum()) {
            // whether mapping is necessary?
            final Object[] constants = enumClass.getEnumConstants();
            if (constants.length < ecd.getValues().length)
                throw new IllegalStateException(enumClass.getName() + " is not corresponded to EnumClassDescriptor mapping");

            boolean isOrdinalChanged = false;
            for (EnumValue enumValue : ecd.getValues()) {
                @SuppressWarnings("unchecked")
                final Enum e = Enum.valueOf((Class<Enum>) enumClass, enumValue.getNormalizedSymbol());
                if (e == null)
                    throw new IllegalStateException(enumClass.getName() + " has no enum constant " + enumValue.symbol);
                if (e.ordinal() != enumValue.value) {
                    isOrdinalChanged = true;
                    break;
                }
            }

            if (isOrdinalChanged) {
                final int[] map = new int[constants.length];
                for (EnumValue enumValue : ecd.getValues()) {
                    @SuppressWarnings("unchecked")
                    final Enum e = Enum.valueOf((Class<Enum>) enumClass, enumValue.symbol);
                    map[e.ordinal()] = (int) enumValue.value;
                }
                return map;
            } else
                return null;
        }else return null;
    }



    private static boolean isJavaFlagsEnumConstant(Field f) {
        int mod = f.getModifiers();
        
        return (Modifier.isStatic(mod) && Modifier.isFinal(mod) && f.getType() == f.getDeclaringClass ());
    }


    public static int compareNulls(boolean isNull1, boolean isNull2) {
        if (isNull1)
            if (isNull2)
                return 0;
            else
                return -1;
        else if (isNull2)
            return 1;
        else
            return 2;
    }

    public static String getTypeBySize(int size) {
        switch (size) {
            case 1:
                return "Byte";
            case 2:
                return "Short";
            case 4:
                return "Int";
            case 6:
            case 8:
                return "Long";
            default:
                throw new IllegalStateException(String.valueOf(size));
        }
    }

    public static String primitiveTypeBySize(int size) {
        switch (size) {
            case 1:
                return "byte";
            case 2:
                return "short";
            case 4:
                return "int";
            case 6:
            case 8:
                return "long";
            default:
                throw new IllegalStateException(String.valueOf(size));
        }
    }

    public static Class<?> getPrimitiveClass(String className) {
        if ("long".equals(className))
            return long.class;
        else if ("int".equals(className))
            return int.class;
        else if ("short".equals(className))
            return short.class;
        else if ("byte".equals(className))
            return byte.class;
        else if ("float".equals(className))
            return float.class;
        else if ("double".equals(className))
            return double.class;
        else if ("char".equals(className))
            return char.class;
        if ("fromString".equals(className))
            return String.class;
        if ("null".equals(className))
            return null;
        else
            throw new IllegalStateException(className);
    }

    public static Number getMinLimit(DataType dataType, boolean isBound, Class<?> valueClazz) {
        if (dataType instanceof IntegerDataType) {
            final Class<?> nativeType = RecordLayout.getNativeType(dataType);
            final long fieldTypeMin = getLimit4BaseClass(false, nativeType);
            final long inTypeMin = ((isBound && isTheSameSize(nativeType, valueClazz)) ||
                    valueClazz == String.class) ? fieldTypeMin : getLimit4BaseClass(false, valueClazz);
            final Number min = ((IntegerDataType) dataType).getMin();
            final int idx;
            return (min != null && min.longValue() > inTypeMin) ? min :
                    (min == null && IntegerDataType.MINS[idx = ((IntegerDataType) dataType).getIndex()] > inTypeMin) ?
                    IntegerDataType.MINS[idx] : null;
        } else if (dataType instanceof FloatDataType) {
            final Number min = ((FloatDataType) dataType).getMin();
            final double inTypeMin = -(valueClazz == float.class ? Float.MAX_VALUE : Double.MAX_VALUE);
            return (min != null && min.doubleValue() > inTypeMin) ? min :
                    (valueClazz == String.class) ||
                            (min == null && valueClazz == double.class && ((FloatDataType) dataType).isFloat()) ?
                            -Float.MAX_VALUE : null;
        } else
            throw new IllegalStateException(dataType.toString());
    }

    public static Number getMaxLimit(DataType dataType, boolean isBound, Class<?> valueClazz) {
        if (dataType instanceof IntegerDataType) {
            final Class<?> nativeType = RecordLayout.getNativeType(dataType);
            final long fieldTypeMax = getLimit4BaseClass(true, nativeType);
            final long inTypeMax = ((isBound && isTheSameSize(nativeType, valueClazz)) ||
                    valueClazz == String.class) ? fieldTypeMax : getLimit4BaseClass(true, valueClazz);
            final Number max = ((IntegerDataType) dataType).getMax();
            final int idx;
            return (max != null && max.longValue() < inTypeMax) ? max :
                    (max == null && IntegerDataType.MAXS[idx = ((IntegerDataType) dataType).getIndex()] < inTypeMax) ?
                            IntegerDataType.MAXS[idx] : null;
        } else if (dataType instanceof FloatDataType) {
            final Number max = ((FloatDataType) dataType).getMax();
            final double inTypeMax = valueClazz == float.class ? Float.MAX_VALUE : Double.MAX_VALUE;
            return (max != null && max.doubleValue() < inTypeMax) ? max :
                    (valueClazz == String.class) ||
                            (max == null && valueClazz == double.class && ((FloatDataType) dataType).isFloat()) ?
                            Float.MAX_VALUE : null;
        } else
            throw new IllegalStateException(dataType.toString());
    }

    private static boolean isTheSameSize(Class<?> type1, Class<?> type2) {
        return MdUtil.getSize(type1) == MdUtil.getSize(type2);
    }

    public static long getLimit4BaseClass(boolean isUpper, Class<?> clazz) {
        if (clazz == long.class || clazz == String.class) // TODO: I'm not sure
            return isUpper ? Long.MAX_VALUE : (Long.MIN_VALUE + 1);
        else if (clazz == int.class)
            return isUpper ? Integer.MAX_VALUE : (Integer.MIN_VALUE + 1);
        else if (clazz == short.class)
            return isUpper ? Short.MAX_VALUE : (Short.MIN_VALUE + 1);
        else if (clazz == byte.class)
            return isUpper ? Byte.MAX_VALUE : (Byte.MIN_VALUE + 1);
        else
            throw new IllegalStateException(clazz.toString());
    }


    public static void storeFieldSize(int pos, MemoryDataOutput out) {
        final int size = out.getPosition() - pos - 1;
        final int n = MessageSizeCodec.fieldSize(size);
        if (n > 1)
            out.insertSpace(pos, n - 1);

        final int backupPos = out.getPosition() + (n - 1);
        out.seek(pos);
        MessageSizeCodec.write(size, out);
        out.seek(backupPos);
    }

    public static int limitMDI(int size, MemoryDataInput in) {
        final int avail = in.getAvail();
        if(size > avail)
            throw new IllegalArgumentException("newAvail > oldAvail " + size + " > " + avail);

        final int pos = in.getCurrentOffset();
        in.setLimit(pos + size);

        return avail + pos;
    }

    public static RecordLayout createFieldLayout(String fieldName, RecordLayout layout) {
        DataType dt = layout.getField(fieldName).getType();
        if (dt instanceof ClassDataType)
            return new RecordLayout(layout.getLoader(), ((ClassDataType) dt).getFixedDescriptor());
        else if (dt instanceof ArrayDataType)
            return new RecordLayout(layout.getLoader(), ((ClassDataType) ((ArrayDataType) dt).getElementDataType()).getFixedDescriptor());
        else
            throw new IllegalArgumentException("unexpected data type " + dt);
    }

    public static RecordLayout createFieldLayout(String fieldName, RecordLayout layout, int index) {
        DataType dt = layout.getField(fieldName).getType();
        if (dt instanceof ClassDataType)
            return new RecordLayout(layout.getLoader(), ((ClassDataType) dt).getDescriptors()[index]);
        else if (dt instanceof ArrayDataType)
            return new RecordLayout(layout.getLoader(), ((ClassDataType) ((ArrayDataType) dt).getElementDataType()).getDescriptors()[index]);
        else
            throw new IllegalArgumentException("unexpected data type " + dt);
    }

    @SuppressWarnings("unused")
    public static PolyBoundEncoderImpl createPolyEncoder(Class<?>[] encoderClasses, String fieldName, RecordLayout layout) {
        final FixedBoundEncoder[] encoders = new FixedBoundEncoder[encoderClasses.length];
        final DataType dt = layout.getDescriptor().getField(fieldName).getType();
        final RecordClassDescriptor[] rcd = (dt instanceof ClassDataType) ?
                ((ClassDataType) dt).getDescriptors() :
                ((ClassDataType) ((ArrayDataType) dt).getElementDataType()).getDescriptors();
        for (int i = 0; i < encoderClasses.length; i++) {
            RecordLayout rl = new RecordLayout(layout.getLoader(), rcd[i]);
            encoders[i] = (FixedBoundEncoder) Util.newInstanceNoX(encoderClasses[i], rl);
        }

        return new PolyBoundEncoderImpl(encoders);
    }

//    @SuppressWarnings("unused")
//    public static CompoundDecoderImpl createCompoundDecoder(RecordLayout[] layouts, Class<?>[] classes, boolean poly) {
//
//        final FixedExternalDecoder[] decoders = new FixedExternalDecoder[classes.length];
//        for (int i = 0; i < classes.length; i++)
//            decoders[i] = (FixedExternalDecoder) Util.newInstanceNoX(classes[i], layouts[i]);
//
//        return new CompoundDecoderImpl(poly, decoders);
//    }

//    @SuppressWarnings("unused")
//    public static PolyBoundDecoderWithPoolImpl createPolyWithPoolDecoder(Class<?>[] decoderClasses, String fieldName, RecordLayout layout, ObjectPool[] pools) {
//        final BoundDecoder[] decoders = new BoundDecoder[decoderClasses.length];
//        final RecordClassDescriptor[] rcd = ((ClassDataType)((ArrayDataType) layout.getDescriptor().getField(fieldName).getType()).getElementDataType()).getDescriptors();
//        for (int i = 0; i < decoderClasses.length; i++) {
//            RecordLayout rl = new RecordLayout(layout.getLoader(), rcd[i]);
//            decoders[i] = (BoundDecoder) Util.newInstanceNoX(decoderClasses[i], rl);
//        }
//
//        return new PolyBoundDecoderWithPoolImpl(decoders, pools);
//    }

    @SuppressWarnings("unused")
    public static <V> V get(IntegerToObjectHashMap<V> map, int key) throws NotFoundException {
        final V value = map.get(key, null);
        if (value == null)
            throw new NotFoundException(String.valueOf(key));
        else
            return value;
    }

    // validate that the bound class contains all NotNull fields (necessary to create an encoder)
    public static void              validateBoundClass(RecordClassInfo layout) {
        NonStaticFieldInfo[] fields = layout.getNonStaticFields();

        for (int i = 0; fields != null && i < fields.length; i++) {
            NonStaticFieldInfo f = fields[i];
            if (!f.getType().isNullable() && !f.isBound())
                throw new IllegalArgumentException(String.format("Encoder creation failed, because the bound class %s doesn't contain NOT NULL field %s.%s",
                        layout.getTargetClass().getName(), layout.getDescriptor().getName(), f.getName()));
        }
    }
}