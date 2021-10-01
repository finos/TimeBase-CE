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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions;

import com.epam.deltix.qsrv.hf.pub.md.DataType;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class SimpleFunctionDescriptor implements StatelessFunctionDescriptor {

    private final String id;
    private final List<Argument> arguments;
    private final DataType returns;
    private final Class<?> cls;
    private final String method;
    private final int resultIndex;
    private final String resultClassName;
    private final int poolIndex;
    private final int dbIndex;
    private final boolean isBoolean;

    public SimpleFunctionDescriptor(String id, List<Argument> arguments, DataType returns, Class<?> cls, String method,
                                    int resultIndex, String resultClassName, int poolIndex, int dbIndex, boolean isBoolean) {
        this.id = id;
        this.arguments = arguments;
        this.returns = returns;
        this.cls = cls;
        this.method = method;
        this.resultIndex = resultIndex;
        this.resultClassName = resultClassName;
        this.poolIndex = poolIndex;
        this.dbIndex = dbIndex;
        this.isBoolean = isBoolean;
    }

    public static SimpleFunctionDescriptor create(Class<?> cls, Method method) {
        String methodName = method.getName();
        int resultIndex = ReflectionUtils.resultIndex(method);
        int poolIndex = ReflectionUtils.poolIndex(method);
        int dbIndex = ReflectionUtils.dbIndex(method);
        boolean isBoolean = method.getReturnType() == boolean.class;
        String id = ReflectionUtils.extractId(method);
        List<Argument> args = ReflectionUtils.introspectComputeMethod(method, Collections.emptyList());
        DataType returnType = ReflectionUtils.extractType(method);
        String returnTypeName = ReflectionUtils.extractTypeName(method);
        return new SimpleFunctionDescriptor(id, args, returnType, cls, methodName, resultIndex, returnTypeName, poolIndex, dbIndex, isBoolean);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public List<Argument> arguments() {
        return arguments;
    }

    @Override
    public DataType returnType() {
        return returns;
    }

    @Override
    public FunctionType functionType() {
        return FunctionType.SIMPLE;
    }

    @Override
    public Class<?> getCls() {
        return cls;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public int getResultIndex() {
        return resultIndex;
    }

    @Override
    public int getPoolIndex() {
        return poolIndex;
    }

    @Override
    public int getDBIndex() {
        return dbIndex;
    }

    @Override
    public String resultClassName() {
        return resultClassName;
    }

    @Override
    public boolean isBoolean() {
        return isBoolean;
    }
}