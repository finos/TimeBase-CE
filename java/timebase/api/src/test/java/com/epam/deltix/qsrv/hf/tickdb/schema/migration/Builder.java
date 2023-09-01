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

package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.util.Arrays;
import java.util.Collections;

/**
 * Helper class
 */
public class Builder {
    public static NonStaticField createNonStatic(String title, String name, FieldType type) {
        NonStaticField field = new NonStaticField();
        field.setTitle(title);
        field.setName(name);
        field.setType(type);
        field.setIsPrimaryKey(false);

        return field;
    }

    public static TypeDescriptor createDescriptor(String title, String name, ObjectArrayList<Field> fields) {
        TypeDescriptor descriptor = new TypeDescriptor();

        descriptor.setTitle(title);
        descriptor.setName(name);
        descriptor.setFields(fields);
        descriptor.setIsAbstract(false);
        descriptor.setIsContentClass(false);

        return descriptor;
    }

    public static TypeDescriptor createDescriptor(String title, String name, Field ... fields) {
        TypeDescriptor descriptor = new TypeDescriptor();

        descriptor.setTitle(title);
        descriptor.setName(name);
        descriptor.setFields(new ObjectArrayList<>(fields));
        descriptor.setIsAbstract(false);
        descriptor.setIsContentClass(false);

        return descriptor;
    }

}
