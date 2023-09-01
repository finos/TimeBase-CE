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

import com.epam.deltix.computations.api.annotations.Signature;
import com.epam.deltix.qsrv.hf.pub.md.DataType;

import java.util.List;

public class FunctionInfoDescriptor implements AnnotatedFunctionDescriptor<Signature> {

    protected final Class<?> cls;
    protected final Signature info;
    protected final FunctionType type;
    protected final DataType[] signature;
    protected final DataType returns;

    public FunctionInfoDescriptor(Class<?> cls, Signature info, FunctionType type) {
        this.cls = cls;
        this.info = info;
        this.type = type;
        this.signature = FunctionDescriptorInfo.extractTypes(info.args());
        this.returns = FunctionDescriptorInfo.extractType(info.returns());
    }

    @Override
    public String id() {
        return info.id();
    }

    @Override
    public List<Argument> arguments() {
        return null; // ToDo
    }

    @Override
    public DataType[] signature() {
        return signature;
    }

    @Override
    public DataType returnType() {
        return returns;
    }

    @Override
    public FunctionType functionType() {
        return type;
    }

    @Override
    public Signature getAnnotation() {
        return info;
    }

    @Override
    public Class<?> getCls() {
        return cls;
    }
}