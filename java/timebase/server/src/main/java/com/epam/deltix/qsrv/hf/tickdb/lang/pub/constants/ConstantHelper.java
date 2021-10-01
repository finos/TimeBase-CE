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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;

public class ConstantHelper {

    public static Expression parseConstant(String constant, DataType dataType) {
        if (constant == null)
            return new Null();
        if (dataType instanceof IntegerDataType) {
            return constant.endsWith("L") ? parseLong(constant): parseInteger(constant);
        } else if (dataType instanceof FloatDataType) {
            if (((FloatDataType) dataType).isDecimal64()) {
                return FloatConstant.parseDecimal(constant);
            } else {
                return FloatConstant.parseDouble(constant);
            }
        } else if (dataType instanceof BooleanDataType) {
            return new BooleanConstant(Boolean.parseBoolean(constant));
        } else if (dataType instanceof CharDataType) {
            return new CharConstant(constant.charAt(0));
        } else if (dataType instanceof DateTimeDataType) {
            return new DateConstant(constant);
        } else {
            throw new UnsupportedOperationException("Unsupported constant type: " + dataType + ", value: " + constant);
        }
    }

    public static IntegerConstant parseInteger(String constant) {
        return new IntegerConstant(Long.parseLong(constant));
    }

    public static LongConstant parseLong(String constant) {
        return new LongConstant(constant);
    }
}