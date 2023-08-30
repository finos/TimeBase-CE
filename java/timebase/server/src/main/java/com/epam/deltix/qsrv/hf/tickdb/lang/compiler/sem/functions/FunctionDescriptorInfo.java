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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataTypeHelper;

import java.util.Arrays;
import java.util.List;

/**
 * QQL function descriptor
 */
public interface FunctionDescriptorInfo {

    /**
     * Function id
     * @return unique function id
     */
    String id();

    List<Argument> arguments();

    DataType returnType();

    FunctionType functionType();

    Class<?> getCls();

    /**
     * Full function signature.
     * @return full
     */
    default DataType[] signature() {
        return arguments().stream().map(Argument::getDataType).toArray(DataType[]::new);
    }

    default boolean isAggregate() {
        return functionType() == FunctionType.AGGREGATION;
    }

    default int accept(DataType[] actualArgTypes) {
        DataType[] signature = signature();
        int numArgs = signature.length;

        if (actualArgTypes.length != numArgs)
            return -1;

        int result = 0;

        for (int i = 0; i < numArgs; i++) {
            if (signature[i] == null || actualArgTypes[i] == null)
                return -1;
            int v = DataTypeHelper.computeDistance(actualArgTypes[i], signature[i]);
            if (v < 0)
                return -1;
            result += v;
        }
        return result;
    }

    static DataType[] extractTypes(String[] args) {
        return Arrays.stream(args).map(String::toUpperCase)
                .map(FunctionDescriptorInfo::extractType)
                .toArray(DataType[]::new);
    }

    static DataType extractType(String arg) {
        DataType dt = ReflectionUtils.forName(arg);
        if (dt == null)
            throw new IllegalArgumentException("Unrecognized type: " + arg);
        return dt;
    }
}