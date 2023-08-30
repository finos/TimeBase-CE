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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;

import static com.epam.deltix.qsrv.hf.pub.md.StandardTypes.*;

/**
 *
 */
public enum SimpleFunctionCode {
    INTEGER_TO_FLOAT,
    INTEGER_TO_DECIMAL,
    DECIMAL_TO_FLOAT,

    VARCHAR_LIKE,
    VARCHAR_NLIKE,
    
    IS_NULL,
    IS_NOT_NULL,

    IS_NAN,
    IS_NOT_NAN,
    ;

    public DataType     getOuputType (CompiledExpression ... args) {
        switch (this) {
            case IS_NULL:
            case IS_NOT_NULL:
            case IS_NAN:
            case IS_NOT_NAN:
                return (CLEAN_BOOLEAN);
        }
        
        boolean             foundNullable = false;

        for (CompiledExpression e : args)
            if (e.type.isNullable ()) {
                foundNullable = true;
                break;
            }

        return (getOuputType (foundNullable));
    }

    private DataType    getOuputType (boolean areArgsNullable) {
        switch (this) {

            case VARCHAR_LIKE:
            case VARCHAR_NLIKE:
                return (areArgsNullable ? NULLABLE_BOOLEAN : CLEAN_BOOLEAN);

            case INTEGER_TO_FLOAT:
            case DECIMAL_TO_FLOAT:
                return (areArgsNullable ? NULLABLE_FLOAT : CLEAN_FLOAT);

            case INTEGER_TO_DECIMAL:
                return areArgsNullable ? NULLABLE_DECIMAL: CLEAN_DECIMAL;

            default:
                throw new UnsupportedOperationException (name ());
        }
    }
}