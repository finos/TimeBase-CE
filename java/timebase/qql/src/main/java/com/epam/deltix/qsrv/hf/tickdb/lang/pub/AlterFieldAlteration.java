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
import com.epam.deltix.util.parsers.Element;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class AlterFieldAlteration extends AttributeDef {
    public final DataTypeSpec type;
    public final Boolean isStatic;
    public final Expression value;
    public final Boolean isNull;
    public final Identifier relativeId;
    public final Expression defval;
    public final String encoding;
    public final Integer dimension;
    public final Expression min;
    public final Expression max;
    public final String newName;

    public AlterFieldAlteration(long location, String id, ArrayList<Element> elements, Expression defval) {
        super(id, extractTitle(elements), extractTags(elements), extractComment(elements), location);
        DataTypeSpec type = null;
        String encoding = null;
        Integer dimension = null;
        Expression min = null, max = null, value = null;
        Identifier relativeId = null;
        Boolean isStatic = null;
        Boolean isNull = null;
        String newName = null;
        for (Element el : elements) {
            if (el instanceof DataTypeSpec) {
                if (type != null) {
                    throw new CompilationException("Duplicate data type definition", el);
                }
                type = (DataTypeSpec) el;
                if (el instanceof SimpleDataTypeSpec) {
                    SimpleDataTypeSpec simpleDataTypeSpec = (SimpleDataTypeSpec) type;
                    if (simpleDataTypeSpec.min != null) {
                        throw new CompilationException("MIN is not supported", el);
                    }
                    if (simpleDataTypeSpec.max != null) {
                        throw new CompilationException("MAX is not supported", el);
                    }
                    if (simpleDataTypeSpec.encoding != null) {
                        encoding = simpleDataTypeSpec.encoding;
                        dimension = simpleDataTypeSpec.dimension;
                    }
                }

                continue;
            }
            if (el instanceof TagsElement) {
                continue;
            }
            OptionElement opt = (OptionElement) el;
            String name = opt.id.id.toUpperCase();
            switch (name) {
                case "ENCODING":
                    if (encoding != null) {
                        throw new CompilationException("Duplicate encoding definition", el);
                    }

                    encoding = opt.value.toString();
                    dimension = 0;
                    int idx = encoding.indexOf('(');
                    if (idx > 0) {
                        int idxDim = encoding.indexOf(')');
                        if (idxDim > 0) {
                            try {
                                dimension = Integer.valueOf(encoding.substring(idx + 1, idxDim).trim());
                                encoding = encoding.substring(0, idx).trim();
                            } catch (NumberFormatException e) {
                                // skip
                            }
                        }
                    }
                    break;
                case "MIN":
                    if (min != null)
                        throw new CompilationException("Duplicate min value definition", el);
                    min = opt.value;
                    break;
                case "MAX":
                    if (max != null)
                        throw new CompilationException("Duplicate max value definition", el);
                    max = opt.value;
                    break;
                case "STATIC":
                    if (isStatic != null)
                        throw new CompilationException("Duplicate static value definition", el);
                    isStatic = true;
                    value = opt.value;
                    break;
                case "NOT STATIC":
                    if (isStatic != null)
                        throw new CompilationException("Duplicate static value definition", el);
                    isStatic = false;
                    break;
                case "NULL":
                    if (isNull != null) {
                        throw new CompilationException("Duplicate null value definition", el);
                    }
                    isNull = true;
                    break;
                case "NOT NULL":
                    if (isNull != null) {
                        throw new CompilationException("Duplicate null value definition", el);
                    }
                    isNull = false;
                    break;
                case "RELATIVE TO":
                    if (relativeId != null) {
                        throw new CompilationException("Duplicate relative definition", el);
                    }
                    if (opt.value instanceof Identifier) {
                        relativeId = (Identifier) opt.value;
                    } else {
                        throw new CompilationException("Identifier required", el);
                    }
                    break;
                case "NAME":
                    if (newName != null)
                        throw new CompilationException("Duplicate new id definition", el);
                    newName = opt.value.toString().replace("\"", "");
                    break;
                case "COMMENT":
                case "DESCRIPTION":
                case "TITLE":
                    break;
                default:
                    throw new CompilationException("Unexpected definition " + opt.id + " " + opt.value, el);
            }
        }
        this.type = type;
        this.defval = defval;
        this.value = value;
        this.min = min;
        this.max = max;
        this.encoding = encoding;
        this.dimension = dimension;
        this.relativeId = relativeId;
        this.isStatic = isStatic;
        this.isNull = isNull;
        this.newName = newName;

//        if (isNull != null && newName != null && !isNull) {
//            throw new CompilationException("SET NOT NULL during changing name is restricted.", location);
//        }
    }

    private static String extractComment(ArrayList<Element> elements) {
        String comment = null;
        for (Element el : elements) {
            if (el instanceof OptionElement) {
                OptionElement opt = (OptionElement) el;
                if (opt.id.toString().equalsIgnoreCase("COMMENT") || opt.id.toString().equalsIgnoreCase("DESCRIPTION")) {
                    if (comment != null) {
                        throw new IllegalArgumentException("Duplicate comment definition");
                    } else {
                        comment = opt.value.toString().replace("'", "");
                    }
                }
            }
        }
        return comment;
    }

    private static String extractTitle(ArrayList<Element> elements) {
        String title = null;
        for (Element el : elements) {
            if (el instanceof OptionElement) {
                OptionElement opt = (OptionElement) el;
                if (opt.id.toString().equalsIgnoreCase("TITLE")) {
                    if (title != null) {
                        throw new IllegalArgumentException("Duplicate title definition");
                    } else {
                        title = opt.value.toString().replace("'", "");
                    }
                }
            }
        }
        return title;
    }

    private static Hashtable<String, String> extractTags(ArrayList<Element> elements) {
        for (Element el : elements) {
            if (el instanceof TagsElement) {
                TagsElement tagsElement = (TagsElement) el;
                return tagsElement.tags;
            }
        }

        return null;
    }

    @Override
    public void print(StringBuilder s) {
        s.append("ALTER FIELD ");
        printHeader(s);

        // todo: print changes
    }
}
