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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatefulFunctionsSet {

    private final String id;
    private final List<StatefulFunctionDescriptor> descriptors = new ArrayList<>();

    public StatefulFunctionsSet(String id) {
        this.id = id;
    }

    public void add(StatefulFunctionDescriptor fd) {
        descriptors.add(fd);
    }

    // long, int | int, long

    public StatefulFunctionDescriptor getDescriptor(DataType[] argTypes, Map<String, DataType> initArgTypes) {
        StatefulFunctionDescriptor ret = null;
        int distance = Integer.MAX_VALUE;
        boolean conflict = false;
        for (StatefulFunctionDescriptor descriptor : descriptors) {
            int v = descriptor.accept(argTypes, initArgTypes);
            if (v >= 0 && v < distance) {
                distance = v;
                ret = descriptor;
                conflict = false;
            } else if (distance == v) {
                conflict = true;
            }
        }
        return conflict ? null: ret;
    }

    public StatefulFunctionDescriptor getDescriptor(DataType[] argTypes, DataType[] initArgTypes) {
        StatefulFunctionDescriptor ret = null;
        int distance = Integer.MAX_VALUE;
        boolean conflict = false;
        for (StatefulFunctionDescriptor descriptor : descriptors) {
            int v = descriptor.accept(argTypes, initArgTypes);
            if (v >= 0 && distance > v) {
                distance = v;
                ret = descriptor;
                conflict = false;
            } else if (distance == v) {
                conflict = true;
            }
        }
        return conflict ? null: ret;
    }

    public String getId() {
        return id;
    }
}