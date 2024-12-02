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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.CompilationException;

public class AlterEnum extends ClassDef {
    public final EnumValueAlteration[] alterations;
    public final TypeIdentifier newName;

    public AlterEnum(long location, TypeIdentifier id, EnumValueAlteration[] alterations, OptionElement[] options) {
        super(location, id, extractTitle(options), null, extractComment(options));
        TypeIdentifier newName = null;
        for (OptionElement opt : options) {
            String name = opt.id.toString().toUpperCase();
            switch (name) {
                case "NAME":
                    if (newName != null)
                        throw new CompilationException("Duplicate new name definition", id);
                    newName = new TypeIdentifier(opt.value.toString().replace("\"", ""));
                    break;
                case "COMMENT":
                case "DESCRIPTION":
                case "TITLE":
                    break;
                default:
                    throw new CompilationException("Unexpected definition " + opt.id + " " + opt.value, id);
            }
        }
        this.alterations = alterations;
        this.newName = newName;
    }

    private static String extractComment(OptionElement[] options) {
        String comment = null;
        for (OptionElement opt : options) {
            if (opt.id.toString().equalsIgnoreCase("COMMENT") || opt.id.toString().equalsIgnoreCase("DESCRIPTION")) {
                if (comment != null) {
                    throw new IllegalArgumentException("Duplicate comment definition");
                } else {
                    comment = opt.value.toString().replace("'", "");
                }
            }
        }
        return comment;
    }

    private static String extractTitle(OptionElement[] options) {
        String title = null;
        for (OptionElement opt : options) {
            if (opt.id.toString().equalsIgnoreCase("TITLE")) {
                if (title != null) {
                    throw new IllegalArgumentException("Duplicate title definition");
                } else {
                    title = opt.value.toString().replace("'", "");
                }
            }
        }
        return title;
    }

    @Override
    public void print(StringBuilder s) {
        s.append("ALTER ENUM ");
        printHeader(s);
        // todo: print alterationslist
    }
}
