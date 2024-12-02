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

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.BooleanConstant;
import com.epam.deltix.util.parsers.CompilationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlterClass extends ClassDef {
    public final AttributeDef[] attributes;
    public final Boolean auxiliary;
    public final Boolean instantiable;
    public final TypeIdentifier parent;
    public final TypeIdentifier newName;
    public final Map<String, Expression> defaultResolutions = new HashMap<>();

    public AlterClass(long location, TypeIdentifier id, List<AttributeDef> alterations, OptionElement[] options, List<?> resolutions) {
        super(location, id, extractTitle(options), null, extractComment(options));
        if (alterations == null) {
            this.attributes = null;
        } else {
            AttributeDef[] atrArray = new AttributeDef[alterations.size()];
            this.attributes = alterations.toArray(atrArray);
        }

        if (resolutions != null) {
            resolutions.forEach(r -> {
                if (r instanceof DefaultFieldResolutionElement) {
                    DefaultFieldResolutionElement fieldResolutionElement = (DefaultFieldResolutionElement) r;
                    defaultResolutions.put(fieldResolutionElement.id.id.toUpperCase(), fieldResolutionElement.e);
                }
            });
        }

        Boolean auxiliary = null, instantiable = null;
        TypeIdentifier parent = null, newName = null;
        for (OptionElement opt : options) {
            String name = opt.id.toString().toUpperCase();
            switch (name) {
                case "AUXILIARY":
                    if (auxiliary != null)
                        throw new CompilationException("Duplicate auxiliary definition", id);
                    auxiliary = ((BooleanConstant) opt.value).value;
                    break;
                case "INSTANTIABLE":
                    if (instantiable != null)
                        throw new CompilationException("Duplicate instantiable definition", id);
                    instantiable = ((BooleanConstant) opt.value).value;
                    break;
                case "UNDER":
                    if (parent != null)
                        throw new CompilationException("Duplicate under definition", id);
                    parent = new TypeIdentifier(opt.value.toString().replace("\"", ""));
                    break;
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
        this.auxiliary = auxiliary;
        this.instantiable = instantiable;
        this.parent = parent;
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
            if (opt.id.toString().equals("TITLE")) {
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
        s.append("ALTER CLASS ");
        printHeader(s);
        s.append(" ");
        if (attributes != null) {
            for (AttributeDef attribute : attributes) {
                attribute.print(s);
                s.append("; \n");
            }
        }

        // todo: print options list
    }
}
