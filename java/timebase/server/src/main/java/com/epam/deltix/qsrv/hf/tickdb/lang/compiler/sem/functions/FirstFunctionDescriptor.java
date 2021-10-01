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

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FirstFunctionDescriptor implements StatefulFunctionDescriptor {

    private final DataType dataType;
    private final List<Argument> arguments;
    private final DataType[] argTypes;
    private final Argument returnValue;

    public FirstFunctionDescriptor(DataType dataType) {
        this.dataType = dataType;
        this.arguments = Collections.singletonList(new Argument("value", dataType, null, null));
        this.argTypes = new DataType[]{dataType};
        this.returnValue = new Argument("first", dataType, null, null);
    }

    @Override
    public String id() {
        return "FIRST";
    }

    @Override
    public List<InitArgument> initArgs() {
        return Collections.emptyList();
    }

    @Override
    public Set<String> initArgNames() {
        return Collections.emptySet();
    }

    @Override
    public DataType[] initArgTypes() {
        return new DataType[0];
    }

    @Override
    public List<Argument> args() {
        return arguments;
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
        return dataType;
    }

    @Override
    public DataType returnType(DataType[] initArgs, DataType[] args) {
        return dataType;
    }

    @Override
    public Class<?> cls() {
        return null;
    }

    @Override
    public String initMethod() {
        return null;
    }

    @Override
    public String computeMethod() {
        return null;
    }

    @Override
    public String resultMethod() {
        return null;
    }

    @Override
    public String resetMethod() {
        return null;
    }

    @Override
    public int timestampIndex() {
        return -1;
    }

    @Override
    public int nanoTimeIndex() {
        return -1;
    }

    @Override
    public int startTimeIndex() {
        return -1;
    }

    @Override
    public int startNanoTimeIndex() {
        return -1;
    }
}