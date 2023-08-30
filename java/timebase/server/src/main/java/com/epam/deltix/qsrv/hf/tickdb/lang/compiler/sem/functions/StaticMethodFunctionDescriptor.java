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

import com.epam.deltix.computations.api.annotations.MultiSignatureFunction;
import com.epam.deltix.computations.api.annotations.Signature;

import java.lang.reflect.Method;

public class StaticMethodFunctionDescriptor extends FunctionInfoDescriptor implements StatelessFunctionDescriptor {

    private final String method;
    private final boolean isBoolean;
    private final int resultIndex;
    private final int poolIndex;

    private StaticMethodFunctionDescriptor(Signature functionInfo, Class<?> cls, String methodName, boolean isBoolean,
                                           int resultIndex, int poolIndex) {
        super(cls, functionInfo, FunctionType.SIMPLE);
        this.method = methodName;
        this.isBoolean = isBoolean;
        this.resultIndex = resultIndex;
        this.poolIndex = poolIndex;
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
        return -1;
    }

    @Override
    public String resultClassName() {
        return null;
    }

    @Override
    public boolean isBoolean() {
        return isBoolean;
    }

    public static StaticMethodFunctionDescriptor[] create(Class<?> cls, Method method) {
        String methodName = method.getName();
        boolean isBoolean = method.getReturnType() == boolean.class;
        int resultIndex = ReflectionUtils.resultIndex(method);
        int poolIndex = ReflectionUtils.poolIndex(method);
        if (method.isAnnotationPresent(MultiSignatureFunction.class)) {
            Signature[] infos = method.getAnnotation(MultiSignatureFunction.class).value();
            StaticMethodFunctionDescriptor[] result = new StaticMethodFunctionDescriptor[infos.length];

            for (int i = 0; i < infos.length; i++) {
                result[i] = new StaticMethodFunctionDescriptor(infos[i], cls, methodName, isBoolean, resultIndex, poolIndex);
            }
            return result;
        } else if (method.isAnnotationPresent(Signature.class)) {
            return new StaticMethodFunctionDescriptor[]{new StaticMethodFunctionDescriptor(
                    method.getAnnotation(Signature.class), cls, methodName, isBoolean, resultIndex, poolIndex)};
        }
        return new StaticMethodFunctionDescriptor[0];
    }
}