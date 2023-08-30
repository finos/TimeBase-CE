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

import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FunctionDescriptor implements StatefulFunctionDescriptor {

    public FunctionDescriptor(String id, List<GenericType> genericTypes, List<InitArgument> initArgs, List<Argument> args,
                              Argument returnValue,
                              Class<?> cls, String initMethod, String computeMethod, String resultMethod,
                              String resetMethod, int timestampIndex, int nanoTimeIndex, int startTimeIndex, int startNanoTimeIndex) {
        this.id = id;
        this.genericTypes = genericTypes;
        this.initArgs = initArgs;
        this.initArgNames = StatefulFunctionDescriptor.argNames(initArgs);
        this.initArgTypes = StatefulFunctionDescriptor.argTypes(initArgs);
        this.args = args;
        this.argTypes = StatefulFunctionDescriptor.argTypes(args);
        this.returnValue = returnValue;
        this.cls = cls;
        this.initMethod = initMethod;
        this.computeMethod = computeMethod;
        this.resultMethod = resultMethod;
        this.resetMethod = resetMethod;
        this.timestampIndex = timestampIndex;
        this.nanoTimeIndex = nanoTimeIndex;
        this.startTimeIndex = startTimeIndex;
        this.startNanoTimeIndex = startNanoTimeIndex;
        int initArgsGenericIndex = -1;
        int argsGenericIndex = -1;
        if (returnValue.isGeneric()) {
            GenericType genericType = returnValue.getGenericType();
            for (int i = 0; i < initArgs.size(); i++) {
                InitArgument arg = initArgs.get(i);
                if (arg.isGeneric() && arg.getGenericType().equals(genericType)) {
                    initArgsGenericIndex = i;
                    break;
                }
            }
            if (initArgsGenericIndex == -1) {
                for (int i = 0; i < args.size(); i++) {
                    Argument arg = args.get(i);
                    if (arg.isGeneric() && arg.getGenericType().equals(genericType)) {
                        argsGenericIndex = i;
                        break;
                    }
                }
            }
        }
        this.initArgsGenericIndex = initArgsGenericIndex;
        this.argsGenericIndex = argsGenericIndex;
    }

    private final String id;
    private final List<GenericType> genericTypes;
    private final List<InitArgument> initArgs;
    private final Set<String> initArgNames;
    private final DataType[] initArgTypes;
    private final List<Argument> args;
    private final DataType[] argTypes;
    private final Argument returnValue;

    // reflection
    private final Class<?> cls;

    private final String initMethod;
    private final String computeMethod;
    private final String resultMethod;
    private final String resetMethod;
    private final int timestampIndex;
    private final int nanoTimeIndex;
    private final int startTimeIndex;
    private final int startNanoTimeIndex;

    private final int initArgsGenericIndex;
    private final int argsGenericIndex;

    @Override
    public String id() {
        return id;
    }

    @Override
    public List<InitArgument> initArgs() {
        return initArgs;
    }

    @Override
    public Set<String> initArgNames() {
        return initArgNames;
    }

    @Override
    public DataType[] initArgTypes() {
        return initArgTypes;
    }

    @Override
    public List<Argument> args() {
        return args;
    }

    @Override
    public DataType[] argTypes() {
        return argTypes;
    }

    @Override
    public Argument returnValue() {
        return returnValue;
    }

    @Override
    public DataType returnType() {
        return returnValue.getDataType();
    }

    @Override
    public DataType returnType(DataType[] initArgs, DataType[] args) {
        if (initArgsGenericIndex != -1) {
            return genericReturnType(initArgs[initArgsGenericIndex]);
        } else if (argsGenericIndex != -1) {
            return genericReturnType(args[argsGenericIndex]);
        }
        return returnType();
    }

    @Override
    public Class<?> cls() {
        return cls;
    }

    @Override
    public String initMethod() {
        return initMethod;
    }

    @Override
    public String computeMethod() {
        return computeMethod;
    }

    @Override
    public String resultMethod() {
        return resultMethod;
    }

    @Override
    public String resetMethod() {
        return resetMethod;
    }

    @Override
    public int timestampIndex() {
        return timestampIndex;
    }

    @Override
    public int nanoTimeIndex() {
        return nanoTimeIndex;
    }

    @Override
    public int startTimeIndex() {
        return startTimeIndex;
    }

    @Override
    public int startNanoTimeIndex() {
        return startNanoTimeIndex;
    }

    private DataType genericReturnType(DataType dataType) {
        DataType genericType = returnType();
        if (genericType instanceof ArrayDataType) {
            boolean elementNullable = ((ArrayDataType) genericType).getElementDataType().isNullable();
            if (dataType instanceof ArrayDataType) {
                return TimebaseTypes.copy((ArrayDataType) dataType, genericType.isNullable(), elementNullable);
            } else if (dataType instanceof ClassDataType) {
                return new ArrayDataType(genericType.isNullable(), TimebaseTypes.copy(dataType, elementNullable));
            } else {
                throw new RuntimeException();
            }
        } else if (genericType instanceof ClassDataType) {
            if (dataType instanceof ArrayDataType) {
                return TimebaseTypes.copy(((ArrayDataType) dataType).getElementDataType(), genericType.isNullable());
            } else if (dataType instanceof ClassDataType) {
                return TimebaseTypes.copy(dataType, genericType.isNullable());
            } else {
                throw new RuntimeException();
            }
        } else {
            throw new RuntimeException();
        }
    }

    private static FunctionDescriptor create(Class<?> cls, Method init) {
        String id = ReflectionUtils.extractId(cls);
        List<GenericType> genericTypes = ReflectionUtils.extractGenericTypes(cls);
        
        String initMethod = init.getName();
        List<InitArgument> initArgs = ReflectionUtils.introspectInitMethod(init, genericTypes);
        int startTimeIndex = ReflectionUtils.startTimeIndex(init);
        int startNanoTimeIndex = ReflectionUtils.startNanoTimeIndex(init);

        Method compute = ReflectionUtils.findComputeMethod(cls);
        String computeMethod = compute.getName();
        List<Argument> args = ReflectionUtils.introspectComputeMethod(compute, genericTypes);
        int timestampIndex = ReflectionUtils.timestampIndex(compute);
        int nanoTimeIndex = ReflectionUtils.nanoTimeIndex(compute);

        Method resultMethod = ReflectionUtils.findResultMethod(cls);
        Argument returnValue = ReflectionUtils.introspectResultMethod(resultMethod, genericTypes);

        Method resetMethod = ReflectionUtils.findResetMethod(cls);

        return new FunctionDescriptor(id, genericTypes, initArgs, args, returnValue, cls, initMethod, computeMethod,
                resultMethod.getName(), resetMethod.getName(), timestampIndex, nanoTimeIndex, startTimeIndex, startNanoTimeIndex);
    }

    private static FunctionDescriptor createEmptyInit(Class<?> cls) {
        String id = ReflectionUtils.extractId(cls);
        List<GenericType> genericTypes = ReflectionUtils.extractGenericTypes(cls);

        String initMethod = null;
        List<InitArgument> initArgs = Collections.emptyList();
        int startTimeIndex = -1;
        int startNanoTimeIndex = -1;

        Method compute = ReflectionUtils.findComputeMethod(cls);
        String computeMethod = compute.getName();
        List<Argument> args = ReflectionUtils.introspectComputeMethod(compute, genericTypes);
        int timestampIndex = ReflectionUtils.timestampIndex(compute);
        int nanoTimeIndex = ReflectionUtils.nanoTimeIndex(compute);

        Method resultMethod = ReflectionUtils.findResultMethod(cls);
        Argument returnValue = ReflectionUtils.introspectResultMethod(resultMethod, genericTypes);

        Method resetMethod = ReflectionUtils.findResetMethod(cls);

        return new FunctionDescriptor(id, genericTypes, initArgs, args, returnValue, cls, initMethod, computeMethod,
                resultMethod.getName(), resetMethod.getName(), timestampIndex, nanoTimeIndex, startTimeIndex, startNanoTimeIndex);
    }

    public static FunctionDescriptor[] create(Class<?> cls) {
        List<Method> initMethods = ReflectionUtils.findInitMethods(cls);
        if (initMethods.isEmpty()) {
            return new FunctionDescriptor[]{createEmptyInit(cls)};
        }
        FunctionDescriptor[] result = new FunctionDescriptor[initMethods.size()];
        for (int i = 0; i < initMethods.size(); i++) {
            result[i] = create(cls, initMethods.get(i));
        }
        return result;
    }
}