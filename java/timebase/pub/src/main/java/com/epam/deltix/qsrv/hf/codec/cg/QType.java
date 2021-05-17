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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.jcg.*;

/**
 *  The superclass of any data value type.
 */
public abstract class QType <T extends DataType> {
    public static final int SIZE_VARIABLE = -1;

    public static QType     forDataType (DataType type) {
        Class <?>               tc = type.getClass ();

        if (tc == IntegerDataType.class)
            return (new QIntegerType ((IntegerDataType) type));

        if (tc == BooleanDataType.class)
            return (new QBooleanType ((BooleanDataType) type));

        if (tc == VarcharDataType.class)
            return (((VarcharDataType) type).getEncodingType() == VarcharDataType.ALPHANUMERIC) ?
                (new QAlphanumericType((VarcharDataType) type)) :
                (new QStringType((VarcharDataType) type));

        if (tc == FloatDataType.class)
            return (new QFloatType ((FloatDataType) type));

        if (tc == DateTimeDataType.class)
            return (new QDateTimeType ((DateTimeDataType) type));

        if (tc == ClassDataType.class)
            return (new QClassType((ClassDataType) type));

        if (tc == EnumDataType.class)
            return (new QEnumType ((EnumDataType) type));

        if (tc == CharDataType.class)
            return (new QCharType ((CharDataType) type));

        if (tc == TimeOfDayDataType.class)
            return (new QTimeOfDayType ((TimeOfDayDataType) type));

        if (tc == BinaryDataType.class)
            return (new QBinaryType((BinaryDataType) type));

        if (tc == ArrayDataType.class)
            return (new QArrayType ((ArrayDataType) type));

        throw new UnsupportedOperationException (tc.getSimpleName ());
    }

    public final T      dt;

    protected QType (T dt) {
        this.dt = dt;
    }

    public void                 skip (
        JExpr                       input,
        JCompoundStatement          addTo
    )
    {
        throw new UnsupportedOperationException (
            getEncodedFixedSize () == SIZE_VARIABLE ?
                "Unimplemented for " + getClass().getSimpleName() :
                "Fixed size - should have been skipped by #bytes"            
        );
    }

    /**
     * @return the isNullable
     */
    public final boolean        isNullable () {
        return dt.isNullable ();
    }

    public abstract int         getEncodedFixedSize ();

    protected boolean hasConstraint() {
        return false;
    }

    protected JExpr makeConstantExpr(Object obj) {
        throw new UnsupportedOperationException (
            "Not implemented for " + getClass ().getSimpleName ()
        );
    }

    public void                 encodeNull (
        JExpr                       output,
        JCompoundStatement          addTo
    )
    {
        if (isNullable())
            encodeNullImpl(output, addTo);
        else
            throw new IllegalStateException("type is not Nullable " + dt.getBaseName());
    }

    protected void              encodeNullImpl(
        JExpr output,
        JCompoundStatement addTo
    ) {
        throwNotImplemented();
    }

    protected void              throwNotImplemented () {
        throw new UnsupportedOperationException (
            "Not implemented for " + getClass ().getSimpleName ()
        );
    }
}
