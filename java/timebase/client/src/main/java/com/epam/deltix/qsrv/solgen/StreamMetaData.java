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
package com.epam.deltix.qsrv.solgen;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StreamMetaData {
    private String nameSpace;
    private Set<RecordClassDescriptor> allTypes = new HashSet<>();
    private List<RecordClassDescriptor> concreteTypes = new ArrayList<>();

    public StreamMetaData(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public boolean addType(RecordClassDescriptor descriptor) {
        return allTypes.add(descriptor);
    }

    public boolean addConcreteType(RecordClassDescriptor descriptor) {
        return concreteTypes.add(descriptor);
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public Set<RecordClassDescriptor> getAllTypes() {
        return allTypes;
    }

    public List<RecordClassDescriptor> getConcreteTypes() {
        return concreteTypes;
    }
}