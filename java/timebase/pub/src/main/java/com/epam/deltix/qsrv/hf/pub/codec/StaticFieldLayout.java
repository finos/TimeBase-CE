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
package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.pub.md.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 */
public class StaticFieldLayout
    extends FieldLayout <StaticDataField> 
    implements StaticFieldInfo
{
    public StaticFieldLayout (RecordClassDescriptor owner, StaticDataField field) {
        super (owner, field);
    }


    public void                 set (Object msgObject)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {


        // #7871: silently skip unbound field
        if(!isBound())
            return;

        if (!hasAccessMethods())
            setUsingField(msgObject);
        else
            setUsingMethod(msgObject);
    }

    public void                 setUsingMethod (Object msgObject)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // special handling for IntegerDataType
        final DataType dataType = getType();

        final Method setter = getSetter();
        // #7871: silently skip unbound field
        if(setter == null)
            return;

        final Class<?> fieldType = getSetterType();
        final Class<?> baseType = fieldType;
        final String valueAsString = getField().getStaticValue();

        if (valueAsString == null) {
            if (!dataType.isNullable())
                throw new IllegalArgumentException("Cannot assign null value to non-nullable field " + getName ());

            if (hasNullifier())
                getNullifier().invoke(msgObject);
            else
                setter.invoke (msgObject, MdUtil.getNullValue (dataType, fieldType));
        }
        else if (dataType instanceof IntegerDataType) {
            final long value = ((Number) getValue()).longValue();

            final Number min = CodecUtils.getMinLimit(dataType, false, baseType);
            final Number max = CodecUtils.getMaxLimit(dataType, false, baseType);
            final Number baseMin = CodecUtils.getLimit4BaseClass(false, baseType);
            final Number baseMax = CodecUtils.getLimit4BaseClass(true, baseType);
            if ((min != null && min.longValue() > value) ||
                    (baseMin != null && baseMin.longValue() > value) ||
                    (max != null && max.longValue() < value) ||
                    (baseMax != null && baseMax.longValue() < value)
                    ) {
                throw new IllegalArgumentException("Static value is out of range. Field " + getName() + " value " + value);
            }

            if (fieldType == int.class)
                setter.invoke (msgObject, (int) value);
            else if (fieldType == long.class)
                setter.invoke (msgObject, value);
            else if (fieldType == short.class)
                setter.invoke (msgObject, (short) value);
            else if (fieldType == byte.class)
                setter.invoke (msgObject, (byte) value);
            else
                throw new RuntimeException(fieldType.getName());

        } else if (dataType instanceof EnumDataType) {
            setter.invoke(msgObject, getEnum());
        } else if (dataType instanceof VarcharDataType) {
            if (baseType == long.class) {
                final int len = ((VarcharDataType) dataType).getLength();
                final String value = getField().getStaticValue();
                final long encodedValue = (value != null) ?
                        ("-9223372036854775808".equals(value) ? ExchangeCodec.NULL : // temporary hack
                                len == 10 ? ExchangeCodec.codeToLong(value) : new AlphanumericCodec(len).encodeToLong(value)) :
                        ExchangeCodec.NULL;

                setter.invoke (msgObject,  encodedValue);
            }
            else
                setter.invoke(msgObject, getValue());
        } else if (dataType instanceof BooleanDataType && (fieldType == byte.class)) {
            final Boolean     v = (Boolean) getValue ();
            byte        bv = v ? BooleanDataType.TRUE : BooleanDataType.FALSE;
            setter.invoke(msgObject, bv);
        }
        else
            setter.invoke(msgObject, getValue());
    }


    public void                 setUsingField (Object msgObject)
        throws IllegalArgumentException, IllegalAccessException 
    {
        // special handling for IntegerDataType
        final DataType dataType = getType();

        final Field field = getJavaField();
        // #7871: silently skip unbound field
        if(field == null)
            return;

        final Class<?> fieldType = field.getType();
        final Class<?> baseType = fieldType;
        final String valueAsString = getField().getStaticValue();
        
        if (valueAsString == null) {
            if (!dataType.isNullable())
                throw new IllegalArgumentException("Cannot assign null value to non-nullable field " + getName ()); 
            
            set (MdUtil.getNullValue (dataType, fieldType), msgObject);                    
        }
        else if (dataType instanceof IntegerDataType) {
            final long value = ((Number) getValue()).longValue();
           
            final Number min = CodecUtils.getMinLimit(dataType, false, baseType);
            final Number max = CodecUtils.getMaxLimit(dataType, false, baseType);
            final Number baseMin = CodecUtils.getLimit4BaseClass(false, baseType);
            final Number baseMax = CodecUtils.getLimit4BaseClass(true, baseType);
            if ((min != null && min.longValue() > value) ||
                (baseMin != null && baseMin.longValue() > value) ||
                (max != null && max.longValue() < value) ||
                (baseMax != null && baseMax.longValue() < value)
                ) {
                throw new IllegalArgumentException("Static value is out of range. Field " + getName() + " value " + value);
            }
                      
            if (fieldType == int.class)
                field.setInt (msgObject, (int) value);
            else if (fieldType == long.class)
                field.setLong (msgObject, value);
            else if (fieldType == short.class)
                field.setShort (msgObject, (short) value);
            else if (fieldType == byte.class)
                field.setByte (msgObject, (byte) value);
            else
                throw new RuntimeException(fieldType.getName());
        } else if (dataType instanceof EnumDataType) {
                set(getEnum(), msgObject);
        } else if (dataType instanceof VarcharDataType) {
            if (baseType == long.class) {
                final int len = ((VarcharDataType) dataType).getLength();
                final String value = getField().getStaticValue();
                final long encodedValue = (value != null) ?
                        ("-9223372036854775808".equals(value) ? ExchangeCodec.NULL : // temporary hack
                                len == 10 ? ExchangeCodec.codeToLong(value) : new AlphanumericCodec(len).encodeToLong(value)) :
                        ExchangeCodec.NULL;
                field.setLong(msgObject, encodedValue);
            }
            else
                set(getValue(), msgObject);
        } else if (dataType instanceof BooleanDataType && (fieldType == byte.class)) {
            final Boolean     v = (Boolean) getValue ();
            byte        bv = v ? BooleanDataType.TRUE : BooleanDataType.FALSE;
            set(bv, msgObject);
        }
        else
            set(getValue(), msgObject);
    }
    
    public Object               getValue () {
        return (getField ().getType ().parse (getField ().getStaticValue ()));
    }

    public boolean              getBoolean () {
        return (DataType.parseBoolean (getField ().getStaticValue ()));
    }

    public int                  getInt () {
        return (DataType.parseInt (getField ().getStaticValue ()));
    }

    public long                 getLong () {
        return (DataType.parseLong (getField ().getStaticValue ()));
    }

    public float                getFloat () {
        return (DataType.parseFloat (getField ().getStaticValue ()));
    }

    public double               getDouble () {
        return (DataType.parseDouble (getField ().getStaticValue ()));
    }

    public String               getString () {
        return (getField ().getStaticValue ());
    }

    private Object getEnum() {
        final Field field = getJavaField();
        if (field != null) {
            final Class<?> type = field.getType();
            final Number value = (Number) getValue();
            if (value == null)
                return MdUtil.isIntegerType(type) ?
                        getEnum(type, EnumDataType.NULL) :
                        null;

            return getEnum(type, value);
        } else if (hasAccessMethods()) {
            final Class<?> type = getSetterType();
            final Number value = (Number) getValue();
            if (value == null)
                return MdUtil.isIntegerType(type) ?
                        getEnum(type, EnumDataType.NULL) :
                        null;

            return getEnum(type, value);
        } else
            return getField().getStaticValue();
    }

    private Object getEnum(Class<?> type, Number value) {
        if (value == null)
            return null;
        else if (type == byte.class)
            return value.byteValue();
        else if (type == short.class)
            return value.shortValue();
        else if (type == int.class)
            return value.intValue();
        else if (type == long.class)
            return value.longValue();
        else if (type == CharSequence.class || type == String.class)
            return getString();
        else {
            return  type.getEnumConstants()[value.intValue()];
        }
    }
}