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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;

import java.util.Objects;

import static com.epam.deltix.qsrv.hf.pub.md.StandardTypes.*;

public class DataTypeHelper {

    public static int computeDistance(DataType source, DataType target) {
        if (NumericType.isNumericType(source) && NumericType.isNumericType(target)) {
            return NumericType.computeDistance(source, target);
        } else if (source instanceof EnumDataType && target instanceof IntegerDataType) {
            return 0;
        } else if (source instanceof ArrayDataType && target instanceof ArrayDataType) {
            DataType sourceElement = ((ArrayDataType) source).getElementDataType();
            DataType targetElement = ((ArrayDataType) target).getElementDataType();
            if (sourceElement instanceof EnumDataType && targetElement instanceof IntegerDataType) {
                return 0;
            }

            return sourceElement.getClass() == targetElement.getClass() &&
                Objects.equals(sourceElement.getEncoding(), targetElement.getEncoding()) ? 0 : -1;
        }
        return target.getClass() == source.getClass() ? 0: -1;
    }

    public static int hashcode(DataType dataType) {
        int result = dataType.getClass().hashCode();
        if (dataType instanceof ArrayDataType) {
            result = 31 * result + hashcode(((ArrayDataType) dataType).getElementDataType());
        } else {
            result = 31 * result + (dataType.getEncoding() == null ? 0 : dataType.getEncoding().hashCode());
        }
        return result;
    }

    public static boolean isEqual(DataType dt1, DataType dt2) {
        if (dt1 == null || dt2 == null)
            return dt1 == dt2;
        return hashcode(dt1) == hashcode(dt2);
    }

    public static boolean isNotEqual(DataType dt1, DataType dt2) {
        return !isEqual(dt1, dt2);
    }

    public static boolean isDecimal64(DataType dataType) {
        return dataType instanceof FloatDataType && ((FloatDataType) dataType).isDecimal64();
    }

    public static boolean isElementDecimal64(DataType dataType) {
        return isDecimal64(dataType) ||
                dataType instanceof ArrayDataType && isDecimal64(((ArrayDataType) dataType).getElementDataType());
    }

    public static boolean isVarchar(DataType dataType) {
        return dataType instanceof VarcharDataType;
    }

    public static boolean isElementVarchar(DataType dataType) {
        return isVarchar(dataType) ||
                dataType instanceof ArrayDataType && isVarchar(((ArrayDataType) dataType).getElementDataType());
    }

    public static DataType logicalOperationResult(DataType left, DataType right) {
        boolean isResultNullable = isResultNullable(left, right);
        if (left instanceof ArrayDataType && right instanceof ArrayDataType) {
            return getBooleanArrayType(isResultNullable, isResultNullable(((ArrayDataType) left).getElementDataType(),
                    ((ArrayDataType) right).getElementDataType()));
        } else if (left instanceof ArrayDataType) {
            return getBooleanArrayType(isResultNullable, isResultNullable(((ArrayDataType) left).getElementDataType(), right));
        } else if (right instanceof ArrayDataType) {
            return getBooleanArrayType(isResultNullable, isResultNullable(left, ((ArrayDataType) right).getElementDataType()));
        } else {
            return getBooleanType(isResultNullable);
        }
    }

    public static boolean isTimestampAndInteger(DataType left, DataType right) {
        return TimebaseTypes.isDateTimeOrDateTimeArray(left) && NumericType.isInteger(right) ||
                TimebaseTypes.isDateTimeOrDateTimeArray(right) && NumericType.isInteger(left);
    }

    public static boolean isTimestampAndTimestamp(DataType left, DataType right) {
        return TimebaseTypes.isDateTimeOrDateTimeArray(left) && TimebaseTypes.isDateTimeOrDateTimeArray(right);
    }

}