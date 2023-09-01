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
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Argument {

    private final String name;
    private final DataType dataType;
    private final Class<?> type;
    private final GenericType genericType;

    public Argument(@Nonnull String name, @Nonnull DataType dataType, @Nullable Class<?> type,
                    @Nullable GenericType genericType) {
        validateGeneric(dataType, genericType);
        this.name = name;
        this.dataType = dataType;
        this.type = type;
        this.genericType = genericType;
    }

    public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isGeneric() {
        return genericType != null;
    }

    public GenericType getGenericType() {
        return genericType;
    }

    protected static void validateGeneric(DataType dataType, GenericType genericType) {
        if (genericType == null)
            return;
        if (!TimebaseTypes.isObjectOrObjectArray(dataType))
            throw new IllegalArgumentException(String.format("Type %s couldn't be generic.", dataType));
    }
}