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
package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

class DataTypeToStringUtils {

    static String toString(DataType[] a) {
        if (a == null) {
            return "null";
        }

        int iMax = a.length - 1;
        if (iMax == -1) {
            return "[]";
        }

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(toString(a[i]));
            if (i == iMax) {
                return b.append(']').toString();
            }
            b.append(", ");
        }
    }

    static String toString(DataType type) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getBaseName());
        if (type instanceof ClassDataType) {
            sb.append("[");
            appendDescriptors(sb, ((ClassDataType) type).getDescriptors());
            sb.append("]");
        } else if (type instanceof ArrayDataType) {
            DataType elementType = ((ArrayDataType) type).getElementDataType();
            if (elementType instanceof ClassDataType) {
                sb.append("[");
                appendDescriptors(sb, ((ClassDataType) elementType).getDescriptors());
                sb.append("]");
            } else {
                sb.append("[");
                sb.append(elementType.getBaseName());
                sb.append("]");
            }
        }

        return sb.toString();
    }

    private static void appendDescriptors(StringBuilder sb, RecordClassDescriptor[] descriptors) {
        boolean first = true;
        for (RecordClassDescriptor descriptor : descriptors) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            int idx = descriptor.getName().lastIndexOf(".");
            sb.append(
                idx >= 0 ? descriptor.getName().substring(idx + 1) : descriptor.getName()
            );
        }
    }
}
