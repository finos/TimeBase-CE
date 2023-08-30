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
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler;

import java.util.ArrayList;

/**
 *
 */
public class OverloadedFunctionSet {

    public final String id;
    private final ArrayList<FunctionDescriptorInfo> descriptors = new ArrayList<>();
    private final ArrayList<DataType[]> argMap = new ArrayList<>();

    public OverloadedFunctionSet(String id) {
        this.id = id;
    }

    public void add(FunctionDescriptorInfo fd) {
        descriptors.add(fd);

        int numArgs = fd.signature().length;

        while (argMap.size() <= numArgs)
            argMap.add(null);

        DataType[] union = argMap.get(numArgs);

        if (union == null)
            argMap.set(numArgs, fd.signature().clone());
        else {
            for (int ii = 0; ii < numArgs; ii++)
                union[ii] = QQLCompiler.unionEx(union[ii], fd.signature()[ii]);
        }
    }

    public DataType[] getSignature(int numArgs) {
        if (argMap.size() <= numArgs)
            return (null);

        return (argMap.get(numArgs));
    }

    public FunctionDescriptorInfo getDescriptor(DataType[] argTypes) {
        FunctionDescriptorInfo ret = null;
        int distance = Integer.MAX_VALUE;
        boolean conflict = false;

        for (FunctionDescriptorInfo fd : descriptors) {
            int v = fd.accept(argTypes);
            if (v >= 0 && distance > v) {
                distance = v;
                ret = fd;
                conflict = false;
            } else if (distance == v) {
                conflict = true;
            }
        }

        return conflict ? null: ret;
    }
}