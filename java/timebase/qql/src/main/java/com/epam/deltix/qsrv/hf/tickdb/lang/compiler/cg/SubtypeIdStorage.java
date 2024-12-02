/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import java.util.*;

class SubtypeIdStorage {

    private final RecordClassDescriptor[] types;

    private final Map<RecordClassDescriptor, List<RecordClassDescriptor>> children;

    SubtypeIdStorage(RecordClassDescriptor[] types) {
        this.types = types;
        this.children = buildChildrenMap();
    }

    List<Integer> subtypeIds(RecordClassDescriptor type) {
        List<Integer> indices = new ArrayList<>();
        addTypeIndex(type, indices);
        addChildrenIndices(type, indices);
        return indices;
    }

    boolean[] subtypeIdsFlags(RecordClassDescriptor type) {
        List<Integer> indices = subtypeIds(type);
        boolean[] result = new boolean[types.length];
        for (Integer index : indices) {
            result[index] = true;
        }

        return result;
    }

    private Map<RecordClassDescriptor, List<RecordClassDescriptor>> buildChildrenMap() {
        Set<RecordClassDescriptor> processedTypes = new HashSet<>();
        Map<RecordClassDescriptor, List<RecordClassDescriptor>> children = new HashMap<>();
        for (RecordClassDescriptor type : types) {
            collectChildren(type, children, processedTypes);
        }

        return children;
    }

    private void collectChildren(RecordClassDescriptor type,
                                 Map<RecordClassDescriptor, List<RecordClassDescriptor>> children,
                                 Set<RecordClassDescriptor> processedTypes) {

        if (processedTypes.contains(type)) {
            return;
        }

        RecordClassDescriptor parent = type.getParent();
        if (parent != null) {
            children.computeIfAbsent(parent, k -> new ArrayList<>()).add(type);
            collectChildren(type.getParent(), children, processedTypes);
        }

        processedTypes.add(type);
    }

    private void addTypeIndex(RecordClassDescriptor descriptor, List<Integer> indices) {
        int index = findIndex(descriptor);
        if (index >= 0) {
            indices.add(index);
        }
    }

    private void addChildrenIndices(RecordClassDescriptor type, List<Integer> indices) {
        List<RecordClassDescriptor> currentChildren = children.get(type);
        if (currentChildren != null) {
            for (RecordClassDescriptor child : currentChildren) {
                addTypeIndex(child, indices);
                addChildrenIndices(child, indices);
            }
        }
    }

    private int findIndex(RecordClassDescriptor descriptor) {
        for (int ii = 0; ii < types.length; ii++) {
            if (types[ii] == descriptor) {
                return ii;
            }
        }

        return -1;
    }
}
