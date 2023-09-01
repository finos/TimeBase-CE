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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.lang.DependsOnClass;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
@DependsOnClass(PropertySupport.class)
public abstract class FieldEncoder {
    protected final ValueGetter getter;
    protected final Class <?>   fieldType;
    protected final boolean     isNullable;
    protected final String      fieldName;
    protected final String      fieldDescription;
    protected final String      exceptionDescription;


    protected FieldEncoder(NonStaticFieldLayout f) {
        final Field field = f.getJavaField ();
        if (f.isBound()) {
            if (f.hasAccessMethods()) {
                fieldName = f.getName();
                fieldDescription = f.toString();
                fieldType = f.getGetterReturnType();
            } else {
                fieldName = field.getName();
                fieldDescription = field.toGenericString();
                fieldType = field.getType();
            }
        } else {
            fieldName = f.getName();
            fieldDescription = f.toString();
            fieldType = f.getFieldType();
        }

        isNullable = f.getType().isNullable();
        exceptionDescription = "array element".equals(f.getTitle()) ?
                "' field array element is not nullable" :
                "' field is not nullable";

        if (f.hasAccessMethods())
            getter = new JavaValueGetterMethod(f);
        else
            getter = field != null ? new JavaValueGetter(field) : null;

    }

    protected abstract void    copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException;

    abstract void    writeNull (EncodingContext ctxt);

    void             setBoolean (boolean value, EncodingContext ctxt) {
        throwNotImplemented();
    }

    void             setChar (char value, EncodingContext ctxt) {
        throwNotImplemented();
    }

    void             setByte (byte value, EncodingContext ctxt) {
        throwNotImplemented();
    }

    void             setShort (short value, EncodingContext ctxt) {
        throwNotImplemented();
    }

    void             setInt (int value, EncodingContext ctxt) {
        throwNotImplemented();
    }

    void             setLong (long value, EncodingContext ctxt) {
        throwNotImplemented();
    }

    void             setFloat (float value, EncodingContext ctxt) {
        throwNotImplemented();
    }

    void             setDouble (double value, EncodingContext ctxt) {
        throwNotImplemented();
    }

    void             setArrayLength(int len, EncodingContext ctxt) {
        throwNotImplemented();
    }

    WritableValue    nextWritableElement () {
        throw new UnsupportedOperationException (getClass().getName());
    }

    UnboundEncoder getFieldEncoder(RecordClassDescriptor rcd) {
        throw new UnsupportedOperationException (getClass().getName());
    }

    void             setBinary (byte[] data, int offset, int length, EncodingContext ctxt) {
        throwNotImplemented();
    }

    abstract void    setString (CharSequence value, EncodingContext ctxt);

    protected boolean           isNull (long value) {
        throw new UnsupportedOperationException (getClass().getName());
    }

    boolean                     isNull(CharSequence value) {
        return value == null;
    }

    boolean                     isBound() {
        return getter != null;
    }

    protected abstract boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException;

    private void              throwNotImplemented () {
        throw new UnsupportedOperationException (
            "Not supported for " + getClass ().getSimpleName ()
        );
    }

    protected void throwNotNullableException() {
        throw new IllegalArgumentException("'" + fieldName + exceptionDescription);
    }

    protected void throwConstraintViolationException(Object v ) {
        throw new IllegalArgumentException(fieldDescription + " == " + v);
    }
}