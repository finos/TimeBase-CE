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
package com.epam.deltix.computations.arrays;

import com.epam.deltix.computations.api.annotations.FunctionsRepo;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.computations.api.annotations.Signature;
import com.epam.deltix.computations.api.util.FunctionsUtils;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.collections.generated.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@FunctionsRepo
public class Common {

    @Signature(id = "SIZE", args = "ARRAY(INT8?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(INT16?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(INT32?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(INT64?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(FLOAT32?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(FLOAT64?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(DECIMAL?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(BOOLEAN?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(CHAR?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(TIMESTAMP?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(TIMEOFDAY?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(ALPHANUMERIC?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(VARCHAR?)?", returns = "INT32?")
    @Signature(id = "SIZE", args = "ARRAY(OBJECT?)?", returns = "INT32?")
    public static int size(@Nullable List<?> list) {
        return list == null ? IntegerDataType.INT32_NULL : list.size();
    }


    @Signature(id = "ISEMPTY", args = "ARRAY(INT8?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(INT16?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(INT32?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(INT64?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(FLOAT32?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(FLOAT64?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(DECIMAL?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(BOOLEAN?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(CHAR?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(TIMESTAMP?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(TIMEOFDAY?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(ALPHANUMERIC?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(VARCHAR?)?", returns = "BOOLEAN")
    @Signature(id = "ISEMPTY", args = "ARRAY(OBJECT?)?", returns = "BOOLEAN")
    public static byte isEmpty(@Nullable List<?> list) {
        return list == null ? BooleanDataType.TRUE : FunctionsUtils.bpos(list.isEmpty());
    }


    @Signature(id = "NOTEMPTY", args = "ARRAY(INT8?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(INT16?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(INT32?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(INT64?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(FLOAT32?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(FLOAT64?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(DECIMAL?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(BOOLEAN?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(CHAR?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(TIMESTAMP?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(TIMEOFDAY?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(ALPHANUMERIC?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(VARCHAR?)?", returns = "BOOLEAN")
    @Signature(id = "NOTEMPTY", args = "ARRAY(OBJECT?)?", returns = "BOOLEAN")
    public static byte notEmpty(@Nullable List<?> list) {
        return list == null ? BooleanDataType.FALSE : FunctionsUtils.bneg(list.isEmpty());
    }

    @Signature(id = "SORT", args = "ARRAY(INT8?)?", returns = "ARRAY(INT8?)?")
    @Signature(id = "SORT", args = "ARRAY(INT16?)?", returns = "ARRAY(INT16?)?")
    @Signature(id = "SORT", args = "ARRAY(INT32?)?", returns = "ARRAY(INT32?)?")
    @Signature(id = "SORT", args = "ARRAY(INT64?)?", returns = "ARRAY(INT64?)?")
    @Signature(id = "SORT", args = "ARRAY(FLOAT32?)?", returns = "ARRAY(FLOAT32?)?")
    @Signature(id = "SORT", args = "ARRAY(FLOAT64?)?", returns = "ARRAY(FLOAT64?)?")
    @Signature(id = "SORT", args = "ARRAY(BOOLEAN?)?", returns = "ARRAY(BOOLEAN?)?")
    @Signature(id = "SORT", args = "ARRAY(CHAR?)?", returns = "ARRAY(CHAR?)?")
    @Signature(id = "SORT", args = "ARRAY(TIMESTAMP?)?", returns = "ARRAY(TIMESTAMP?)?")
    @Signature(id = "SORT", args = "ARRAY(TIMEOFDAY?)?", returns = "ARRAY(TIMEOFDAY?)?")
    public static <V, T extends List<V>> boolean sort(@Nullable T list, @Nonnull @Result T result) {
        result.clear();
        if (FunctionsUtils.isNullOrEmpty(list))
            return false;

        result.addAll(list);
        if (result instanceof ByteArrayList) {
            ((ByteArrayList) result).sort();
        } else if (result instanceof ShortArrayList) {
            ((ShortArrayList) result).sort();
        } else if (result instanceof IntegerArrayList) {
            ((IntegerArrayList) result).sort();
        } else if (result instanceof LongArrayList) {
            ((LongArrayList) result).sort();
        } else if (result instanceof CharacterArrayList) {
            ((CharacterArrayList) result).sort();
        } else if (result instanceof FloatArrayList) {
            ((FloatArrayList) result).sort();
        } else if (result instanceof DoubleArrayList) {
            ((DoubleArrayList) result).sort();
        } else {
            throw new UnsupportedOperationException();
        }
        return true;
    }

    @Signature(id = "SORT", args = "ARRAY(DECIMAL?)?", returns = "ARRAY(DECIMAL?)?")
    public static boolean sort(@Nullable @Decimal LongArrayList list, @Nonnull @Result @Decimal LongArrayList result) {
        result.clear();
        if (FunctionsUtils.isNullOrEmpty(list))
            return false;

        result.addAll(list);
        result.sort(Decimal64Utils::compareTo);
        return true;
    }

    @Signature(id = "ENUMERATE", args = "ARRAY(INT8?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(INT16?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(INT32?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(INT64?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(FLOAT32?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(FLOAT64?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(DECIMAL?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(BOOLEAN?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(CHAR?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(TIMESTAMP?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(TIMEOFDAY?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(ALPHANUMERIC?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(VARCHAR?)?", returns = "ARRAY(INT32)?")
    @Signature(id = "ENUMERATE", args = "ARRAY(OBJECT?)?", returns = "ARRAY(INT32)?")
    public static boolean enumerate(@Nullable List<?> list, @Nonnull @Result IntegerArrayList result) {
        result.clear();
        if (list == null)
            return false;
        result.setSize(list.size());
        for (int i = 0; i < list.size(); i++) {
            result.set(i, i);
        }
        return true;
    }

}