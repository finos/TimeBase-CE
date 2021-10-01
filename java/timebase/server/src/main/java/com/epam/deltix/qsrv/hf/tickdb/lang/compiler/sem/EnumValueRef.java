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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.lang.Util;

/**
 *
 */
public class EnumValueRef {
    public final EnumClassDescriptor       parent;
    public final EnumValue                 field;

    public EnumValueRef (EnumClassDescriptor parent, EnumValue field) {
        this.parent = parent;
        this.field = field;
    }

    @Override
    public boolean          equals (Object obj) {
        if (this == obj)
            return (true);

        if (!(obj instanceof EnumValueRef))
            return (false);

        final EnumValueRef      other = (EnumValueRef) obj;

        return (parent.equals (other.parent) && Util.xequals (field.symbol, other.field.symbol));
    }

    @Override
    public int              hashCode () {
        return (parent.hashCode () + Util.hashCode (field.symbol));
    }
}