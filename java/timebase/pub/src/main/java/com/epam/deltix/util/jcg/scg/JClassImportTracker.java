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
package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.lang.*;

import java.util.*;

/**
 *
 */
public final class JClassImportTracker implements ImportTracker {

    private static class GenericTypeInfo {

        private final String fullName;            // a.b.Type<b.c.Generic>
        private final String simpleName;          // Type<Generic>

        private final String baseName;            // a.b.Type
        private final String baseSimpleName;      // Type

        private final String genericName;         // b.c.Generic
        private final String genericSimpleName;   // Generic

        private GenericTypeInfo(String name) {
            this.fullName = name;

            int genericIndexStart = name.indexOf('<');
            int genericIndexEnd = name.indexOf('>');
            if (genericIndexStart >= 0 && genericIndexEnd > genericIndexStart) {
                genericName = name.substring(genericIndexStart + 1, genericIndexEnd);
                baseName = name.substring(0, genericIndexStart);
                genericSimpleName = Util.getSimpleName(genericName);
                baseSimpleName = Util.getSimpleName(baseName);

                simpleName = baseSimpleName + "<" + genericSimpleName + ">";
            } else {
                baseName = name;
                simpleName = baseSimpleName = Util.getSimpleName(name);
                genericName = null;
                genericSimpleName = null;
            }
        }

    }

    private final Map<String, String> imports = new HashMap<>();

    @Override
    public String getPrintClassName(String name) {
        GenericTypeInfo typeInfo = new GenericTypeInfo(name);
        if (typeInfo.genericName != null) {
            imports.put(typeInfo.genericSimpleName, typeInfo.genericName);
        }

        String sn = typeInfo.simpleName;
        String importName = typeInfo.baseName;
        String fn = imports.get(sn);

        if (fn == null) {
            imports.put(sn, importName);
            return (sn);
        }

        if (fn.equals(importName))
            return (sn);
        //
        //  Conflict
        //
        return (importName);
    }

    @Override
    public void printImports(String pack, StringBuilder out) {
        boolean hasImports = false;

        for (String s : imports.values()) {
            String spack = Util.getPackage(s);

            if (spack.equals(pack) || spack.equals("java.lang"))
                continue;

            hasImports = true;
            out.append("import ");
            out.append(s);
            out.append(";\n");
        }

        if (hasImports)
            out.append('\n');
    }
}