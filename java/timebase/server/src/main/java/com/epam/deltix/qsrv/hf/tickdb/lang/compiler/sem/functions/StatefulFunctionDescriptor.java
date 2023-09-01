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
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledExpression;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface StatefulFunctionDescriptor {

    String id();

    List<InitArgument> initArgs();

    Set<String> initArgNames();

    DataType[] initArgTypes();

    List<Argument> args();

    DataType[] argTypes();

    Argument returnValue();

    DataType returnType();

    DataType returnType(DataType[] initArgs, DataType[] args);

    // reflection

    Class<?> cls();

    String initMethod();

    default boolean isInitPresent() {
        return initMethod() != null;
    }

    String computeMethod();

    String resultMethod();

    String resetMethod();

    int timestampIndex();

    int nanoTimeIndex();

    int startTimeIndex();

    default int computeLength() {
        int result = args().size();
        if (nanoTimeIndex() != -1)
            result++;
        if (timestampIndex() != -1)
            result++;
        return result;
    }

    int startNanoTimeIndex();

    default int initLength() {
        int result = initArgs().size();
        if (startNanoTimeIndex() != -1)
            result++;
        if (startTimeIndex() != -1)
            result++;
        return result;
    }

    /**
     * Computes 'distance' between signatures
     * @param actualArgTypes actual types of arguments
     * @param actualInitArgTypes actual types of init arguments
     * @return -1, when function is incompatible. 0, when function is fully compatible (signatures are equal),
     * greater than 0, when function is compatible, but some args should be converted without loss.
     */
    default int accept(DataType[] actualArgTypes, DataType[] actualInitArgTypes) {
        if (actualArgTypes.length != args().size() || actualInitArgTypes.length != initArgs().size()) {
            return -1;
        }

        int result = 0;
        for (int i = 0; i < args().size(); i++) {
            if (actualArgTypes[i] == null)
                return -1;
            int v = DataTypeHelper.computeDistance(actualArgTypes[i], args().get(i).getDataType());
            if (v < 0)
                return -1;
            result += v;
        }

        for (int i = 0; i < initArgs().size(); i++) {
            if (actualInitArgTypes[i] == null)
                return -1;
            int v = DataTypeHelper.computeDistance(actualInitArgTypes[i], initArgs().get(i).getDataType());
            if (v < 0)
                return -1;
            result += v;
        }

        return result;
    }

    //    f{name1: arg1, name2: arg2}(v1, v2)
//        f{}(v1, v2)
    /*
    f: n1, n2
    f{n1: v1}
     */
    default int accept(DataType[] actualArgTypes, Map<String, DataType> actualInitArgTypes) {
        if (actualArgTypes.length != args().size()) {
            return -1;
        }

        int result = 0;
        for (int i = 0; i < args().size(); i++) {
            if (actualArgTypes[i] == null)
                return -1;
            int v = DataTypeHelper.computeDistance(actualArgTypes[i], args().get(i).getDataType());
            if (v < 0)
                return -1;
            result += v;
        }

        for (String s : actualInitArgTypes.keySet()) {
            if (!initArgNames().contains(s)) {
                return -1;
            }
        }

        for (InitArgument initArg : initArgs()) {
            if (initArg.isRequired()) {
                DataType dataType = actualInitArgTypes.get(initArg.getName());
                if (dataType == null)
                    return -1;
                int v = DataTypeHelper.computeDistance(dataType, initArg.getDataType());
                if (v < 0)
                    return -1;
                result += v;
            } else {
                DataType dataType = actualInitArgTypes.get(initArg.getName());
                if (dataType == null)
                    continue;
                int v = DataTypeHelper.computeDistance(dataType, initArg.getDataType());
                if (v < 0)
                    return -1;
                result += v;
            }
        }
        return result;
    }

    default void print(StringBuilder out, CompiledExpression<?>[] compiledInitArgs, CompiledExpression<?>[] compiledArgs) {
        out.append(id());
        out.append("{");
        if (!initArgs().isEmpty()) {
            out.append(initArgs().get(0).getName()).append(": ");
            compiledInitArgs[0].print(out);
            for (int i = 1; i < initArgs().size(); i++) {
                out.append(", ").append(initArgs().get(i).getName()).append(": ");
                compiledInitArgs[i].print(out);
            }
        }
        out.append("}(");
        if (compiledArgs.length != 0) {
            compiledArgs[0].print(out);
            for (int i = 1; i < args().size(); i++) {
                out.append(",");
                compiledArgs[i].print(out);
            }
        }
        out.append(")");
    }

    static Set<String> argNames(List<? extends Argument> list) {
        return list.stream().map(Argument::getName).collect(Collectors.toSet());
    }

    static DataType[] argTypes(List<? extends Argument> list) {
        return list.stream().map(Argument::getDataType).toArray(DataType[]::new);
    }

    static DataType[] types(CompiledExpression<?>[] expressions) {
        return Arrays.stream(expressions).map(e -> e.type).toArray(DataType[]::new);
    }

}