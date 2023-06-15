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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataTypeHelper;

/**
 *
 */
public final class CompiledConstant extends CompiledExpression <DataType> {
    public static final CompiledConstant    B_False =
            new CompiledConstant (
                    StandardTypes.CLEAN_BOOLEAN,
                    false
            );

    public static final CompiledConstant    B_True =
            new CompiledConstant (
                    StandardTypes.CLEAN_BOOLEAN,
                    true
            );

    public static CompiledConstant  trueOrFalse (boolean v) {
        return (v ? B_True : B_False);
    }

    public final Object         value;

    private final boolean decimalLong;

    public CompiledConstant (DataType type, Object value, boolean decimalLong) {
        this(type, value, null, decimalLong);
    }

    public CompiledConstant (DataType type, Object value) {
        this(type, value, false);
    }

    public CompiledConstant (DataType type, Object value, String name) {
        this(type, value, name, false);
    }

    public CompiledConstant (DataType type, Object value, String name, boolean decimalLong) {
        super (type);
        this.value = value;
        this.name = name;
        this.decimalLong = decimalLong;
    }

    @Override
    public void print (StringBuilder out) {
        out.append (value);
    }

    public boolean                  isNull () {
        return (value == null);
    }

    public boolean isNan() {
        if (value instanceof String) {
            return "NaN".equals(value);
        } else if (value instanceof Long && decimalLong) {
            return Decimal64Utils.isNaN((Long) value);
        } else if (value instanceof Double) {
            return Double.isNaN((Double) value);
        } else if (value instanceof Float) {
            return Float.isNaN((Float) value);
        }

        return false;
    }

    public long                     getLong () {
        if (type == StandardTypes.CLEAN_DECIMAL || type == StandardTypes.NULLABLE_DECIMAL) {
            return Decimal64Utils.parse((String) value);
        }
        return ((Number) value).longValue ();
    }

    @Decimal
    public long getDecimalLong() {
        if (value instanceof Long && decimalLong) {
            return (Long) value;
        } else if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return Decimal64Utils.fromLong(((Number) value).longValue());
        } else if (value instanceof String) {
            return Decimal64Utils.parse((String) value);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public byte getByte() {
        return ((Number) value).byteValue();
    }

    public short getShort() {
        return ((Number) value).shortValue();
    }

    public int getInteger() {
        return ((Number) value).intValue();
    }

    public double                   getDouble () {
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else if (value instanceof Long && decimalLong) {
            return Decimal64Utils.toDouble((Long) value);
        } else {
            return ((Number) value).doubleValue();
        }
    }

    public float                    getFloat () {
        if (value instanceof String) {
            return Float.parseFloat((String) value);
        }
        return ((Number) value).floatValue ();
    }

    public boolean                  getBoolean () {
        return (value != null && ((Boolean) value).booleanValue ());
    }

    public char                     getChar () {
        return ((Character) value);
    }

    public String                   getString () {
        return (value.toString ());
    }

    public Object getValue() {
        if (value instanceof String) {
            if (type == StandardTypes.CLEAN_DECIMAL || type == StandardTypes.NULLABLE_DECIMAL) {
                return Decimal64Utils.parse((String) value);
            } else if (type == StandardTypes.CLEAN_FLOAT || type == StandardTypes.NULLABLE_FLOAT) {
                return Double.parseDouble((String) value);
            }
        }
        return value;
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        if (!super.equals (obj))
            return (false);

        CompiledConstant    b = (CompiledConstant) obj;

        if (value == null)
            return (b.value == null);

        return (value.equals (b.value));
    }

    @Override
    public int                      hashCode () {
        int result = super.hashCode();
        result = result * 31 + (value == null ? 772455 : value.hashCode());
        result = result * 31 + DataTypeHelper.hashcode(type);
        return result;
    }
}