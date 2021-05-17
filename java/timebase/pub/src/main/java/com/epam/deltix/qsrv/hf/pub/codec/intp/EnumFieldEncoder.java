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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.codec.EnumAnalyzer;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;
import com.epam.deltix.qsrv.hf.pub.md.MdUtil;
import com.epam.deltix.util.collections.CharSequenceToLongMap;
import com.epam.deltix.util.collections.generated.LongArrayList;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class EnumFieldEncoder extends FieldEncoder {
    private final CharSequenceToLongMap   symbolToValueMap =
        new CharSequenceToLongMap ();
    private final long[]                        map;
    private final LongArrayList                 invalid;
    private final int                           size;
    private final EnumValue[]                   values;
    private final long                          mask;
    
    EnumFieldEncoder (NonStaticFieldLayout f) {
        super (f);

        EnumClassDescriptor ed = ((EnumDataType) f.getType ()).descriptor;

        for (EnumValue ev : ed.getValues ())
            symbolToValueMap.put (ev.symbol, ev.value);

        final Class<?> fieldType = f.getFieldType();
        if (fieldType != null) {
            final EnumAnalyzer ea = new EnumAnalyzer();
            ea.analyze(fieldType, ed, true);
            map = ea.getEnumMap();
            invalid = ea.getBindingMap();
//            if (ea.analyze(fieldType, ed, true)) {
//                map = ea.getEnumMap();
//                invalid = ea.getInvalidOrginals();
//            } else {
//                map = null;
//                invalid = null;
//            }
        } else {
            map = null;
            invalid = null;
        }

//        final Field jf = f.getJavaField();
//        if (jf != null) {
//            final EnumAnalyzer ea = new EnumAnalyzer();
//            if (ea.analyze(jf.getType(), ed, true)) {
//                map = ea.getEnumMap();
//                invalid = ea.getInvalidOrginals();
//            } else {
//                map = null;
//                invalid = null;
//            }
//        } else {
//            map = null;
//            invalid = null;
//        }

        size = ed.computeStorageSize ();
        // exclude case of BITMASK
        values = !ed.isBitmask() ? ed.getValues() : null;
        if (ed.isBitmask()) {
            long m = 0;
            for (EnumValue enumValue : ed.getValues()) {
                m |= enumValue.value;
            }
            mask = ~m;
        } else
            mask = 0;
    }

    public void         encode(Enum value, EncodingContext ctx) {

        long                longValue = EnumDataType.NULL_CODE;

        if (value != null) {
            if (fieldType.isEnum() || MdUtil.isIntegerType(fieldType)) {
                final int ordinal = value.ordinal();

                if (invalid != null && (invalid.contains(ordinal))) //oorginal >= invalid.size() || invalid[orginal]))
                    throw new IllegalArgumentException(String.format("value is absent in schema: %s == %s", fieldName, value));

                longValue = (map != null) ? map[ordinal] : ordinal;
            }
            else if (MdUtil.isStringType(fieldType)) {
                setString(value.toString(), ctx);
                return;
            }
            else
                throw new UnsupportedOperationException("Unsupported field type " + fieldType.getName());
        }

        setLong (longValue, ctx);
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // TODO: avoid unnecessary boxing here
        Object              value = getter.get (obj);
        long                longval;
        
        if (value == null)                  //  Java null
            longval = EnumDataType.NULL_CODE;
        else {
            if (fieldType.isEnum()) {              //  Java Enum instance
                final int orginal = ((Enum) value).ordinal();
                if (invalid != null && (invalid.contains(orginal))) //oorginal >= invalid.size() || invalid[orginal]))
                    throw new IllegalArgumentException(String.format("value is absent in schema: %s == %s", fieldName, value));

                longval = (map != null) ? map[orginal] : orginal;
            }
            else if (MdUtil.isIntegerType(fieldType)) {
                if (fieldType == byte.class)
                    longval = getter.getByte(obj);
                else if (fieldType == short.class)
                    longval = getter.getShort(obj);
                else if (fieldType == int.class)
                    longval = getter.getInt(obj);
                else if (fieldType == long.class)
                    longval = getter.getLong(obj);
                else
                    throw new IllegalStateException(fieldType.getName());
            } else if (MdUtil.isStringType(fieldType)) {
                setString((CharSequence) value, ctxt);
                return;
            }
            else
                throw new UnsupportedOperationException("Unsupported field type " + fieldType.getName());
//            else {                          //  IKVM wrapper around a .Net enum
//                try {
//                    longval = value.getClass ().getDeclaredField ("Value").getLong (value);
//                    if (invalid != null && (longval >= invalid.length || invalid[(int) longval]))
//                        throw new IllegalArgumentException(String.format("value is absent in schema: %s == %s", fieldName, value));
//
//                    if (map != null)
//                        longval = map[(int) longval];
//                } catch (Exception ex) {
//                    if (ex instanceof RuntimeException)
//                        throw (RuntimeException) ex;
//                    else
//                        throw new RuntimeException("value: " + value, ex);
//                }
//            }
        }

        setLong (longval, ctxt);
    }

    @Override
    void                    writeNull(EncodingContext ctxt) {
        setLong(EnumDataType.NULL, ctxt);
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        long    v;
        
        if (value == null)
            v = EnumDataType.NULL_CODE;
        else {
            v = symbolToValueMap.get (value, EnumDataType.NULL_CODE);
            
            if (v == EnumDataType.NULL_CODE) {
                if (mask == 0)
                    throwConstraintViolationException(value);
                
                v = 0;
                final String [] ss = value.toString().split ("\\|");

                for (String s : ss) {
                    v |= symbolToValueMap.get (s, 0);
                }
                if (v == 0)
                    throwConstraintViolationException(value);
            }
        }
        
        setLong (v, ctxt);        
    }

    @Override
    void                    setLong (long value, EncodingContext ctxt) {
        validate(value);

        switch (size) {
            case 1:         ctxt.out.writeByte (value);     break;
            case 2:         ctxt.out.writeShort ((int) value); break;
            case 4:         ctxt.out.writeInt ((int) value); break;
            case 8:         ctxt.out.writeLong (value);     break;
            default:        throw new RuntimeException ();
        }
    }

    @Override
    protected boolean isNull(long value) {
        // used when bound with byte/short/int/long field
        return value == -1;
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        // .NET enum has no null-value
        return fieldType.isEnum() && getter.get(message) == null;
    }

    private void validate(long value) {
        if (value == EnumDataType.NULL) {
            if (!isNullable)
                throwNotNullableException();
            else
                return;
        }

        if (mask != 0) {
            if ((mask & value) == 0)
                return;
        } else {
            for (EnumValue enumValue : values) {
                if (enumValue.value == value)
                    return;
            }
        }

        throwConstraintViolationException(value);
    }
}
