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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public class ClassDataTypeSpec extends PolymorphicDataTypeSpec {
    public ClassDataTypeSpec (
        long                        location,
        DataTypeSpec[]              elementsTypeSpec,
        boolean                     nullable
    )
    {
        super (location, elementsTypeSpec, nullable);
    }

    public ClassDataTypeSpec (
        DataTypeSpec[]              elementTypeSpec,
        boolean                     nullable
    )
    {
        this (Location.NONE, elementTypeSpec, nullable);
    }

    @Override
    public void             print (StringBuilder s) {
        s.append("OBJECT");
        s.append("(");

        boolean first = true;
        for (DataTypeSpec element : elementsTypeSpec) {
            if (first)
                first = false;
            else
                s.append (",");

            element.print (s);
        }

        s.append(")");

        if (!nullable)
            s.append (" NOT NULL");
    }
}