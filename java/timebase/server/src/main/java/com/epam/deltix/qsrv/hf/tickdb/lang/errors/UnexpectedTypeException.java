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
package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *
 */
public class UnexpectedTypeException extends CompilationException {
    public UnexpectedTypeException(Expression e, DataType actual, DataType ... expected) {
        super (
            "Illegal type in: " + e +
                        "; context requires: [" + Arrays.stream(expected).map(DataType::getBaseName).collect(Collectors.joining(",")) +
                        "]; found: " + actual.getBaseName(),
            e
        );
    }

    private static String fullTypeName(DataType type) {
        if (type instanceof ArrayDataType) {
            return type.getBaseName() + "[" + fullTypeName(((ArrayDataType) type).getElementDataType()) + "]";
        } else if (type instanceof ClassDataType) {
            return type.getBaseName() + "{" +
                Arrays.stream(((ClassDataType) type).getDescriptors())
                    .map(NamedDescriptor::getName)
                    .map(n -> n.substring(Math.max(n.lastIndexOf('.') + 1, 0)))
                    .collect(Collectors.joining(","))
                + "}";
        } if (type instanceof IntegerDataType) {
            int size = ((IntegerDataType) type).getNativeTypeSize();
            switch (size) {
                case 1: return "INT8";
                case 2: return "INT16";
                case 4: return "INT32";
                case 8: return "INT64";
            }
        } else if (type instanceof FloatDataType) {
            if (((FloatDataType) type).isDecimal64()) {
                return "DECIMAL";
            } else if (((FloatDataType) type).isFloat()) {
                return "FLOAT32";
            } else {
                return "FLOAT64";
            }
        } else if (type instanceof BooleanDataType) {
            return "BOOL";
        } else if (type instanceof CharDataType) {
            return "CHAR";
        } else if (type instanceof DateTimeDataType) {
            return "TIMESTAMP";
        }

        return type.getBaseName();
    }

}