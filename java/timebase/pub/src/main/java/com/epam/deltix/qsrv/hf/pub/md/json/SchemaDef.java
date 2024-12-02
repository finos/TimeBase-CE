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
package com.epam.deltix.qsrv.hf.pub.md.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Optional;

/**
 * Stream schema definition
 */
public class SchemaDef {

    /**
     * Schema top-types list (used to represent messages)
     */
    @JsonProperty()
    public TypeDef[]   types;

    /**
     * Schema all-types list (including enumeration and nested types)
     */
    @JsonProperty()
    public TypeDef[]   all;

    public TypeDef          find(String name) {
        Optional<TypeDef> type = Arrays.stream(all)
                .filter(s -> s.getName().equals(name))
                .findAny();

        return type.orElse(null);
    }
}