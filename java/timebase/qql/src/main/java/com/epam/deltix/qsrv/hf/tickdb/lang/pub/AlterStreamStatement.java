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
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.IntegerConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants.StringConstant;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.time.Periodicity;

public final class AlterStreamStatement extends Statement {
    public final Identifier id;
    public final ClassDef[] alterations;
    public final ConversionConfirmation confirm;

    public final String key;
    public final String description;
    public final String name;
    public final String owner;
    public final String location;
    public final Long df;
    public final Boolean ha;
    public final Periodicity periodicity;
    public final Boolean versioning;

    // buffer options
    public final Boolean lossy;
    public final Long initSize;
    public final Long maxSize;
    public final Long maxTime;

    public AlterStreamStatement (
            long location,
            Identifier id,
            OptionElement[] options,
            ClassDef[] alterations,
            ConversionConfirmation confirm) {
        super(location);
        this.id = id;
        this.alterations = alterations == null ? new ClassDef [0] : alterations;
        this.confirm = confirm == null ? ConversionConfirmation.NO_CONVERSION : confirm;

        String key = null;
        String description = null, name = null;
        String owner = null;
        String loc = null;
        Long df = null;
        Boolean ha = null;
        Periodicity periodicity = null;
        Boolean versioning = null;
        Boolean lossy = null;
        Long initSize = null;
        Long maxSize = null;
        Long maxTime = null;
        for (OptionElement opt : options) {
            String optionName = opt.id.id.trim().toUpperCase();
            if (optionName.equals("COMMENT") || optionName.equals("DESCRIPTION")) {
                description = getStringValue(opt.value);
                continue;
            } else if (optionName.equals("NAME") && name == null) {
                name = getStringValue(opt.value);
                continue;
            }

            switch (optionName) {
                case "KEY":
                    if (key != null) {
                        throw new CompilationException("Duplicate KEY definition", opt.value);
                    }

                    key = getStringValue(opt.value);
                    break;
                case "HIGH_AVAILABILITY":
                case "HIGHAVAILABILITY":
                case "HA":
                    if (ha != null) {
                        throw new CompilationException("Duplicate HIGHAVAILABILITY definition", opt.value);
                    }

                    ha = getBoolValue(opt.value);
                    break;
                case "OWNER":
                    if (owner != null) {
                        throw new CompilationException("Duplicate OWNER definition", opt.value);
                    }
                    owner = getStringValue(opt.value);
                    break;
                case "LOCATION":
                    if (loc != null) {
                        throw new CompilationException("Duplicate LOCATION definition", opt.value);
                    }
                    loc = getStringValue(opt.value);
                    break;
                case "DF":
                case "DISTRIBUTIONFACTOR":
                case "DISTRIBUTION_FACTOR":
                    if (df != null) {
                        throw new CompilationException("Duplicate DISTRIBUTIONFACTOR definition", opt.value);
                    }
                    df = getIntegerValue(opt.value);
                    break;
                case "PERIODICITY":
                    if (periodicity != null) {
                        throw new CompilationException("Duplicate PERIODICITY definition", opt.value);
                    }
                    try {
                        periodicity = Periodicity.parse(getStringValue(opt.value));
                    } catch (Throwable t) {
                        throw new CompilationException("Invalid PERIODICITY", opt.value);
                    }
                    break;
                case "VERSIONING":
                    if (versioning != null) {
                        throw new CompilationException("Duplicate VERSIONING definition", opt.value);
                    }
                    versioning = getBoolValue(opt.value);
                    if (!versioning) {
                        throw new CompilationException("Invalid versioning value. Support only true", opt.value);
                    }
                    break;
                case "LOSSY":
                    if (lossy != null) {
                        throw new CompilationException("Duplicate LOSSY definition", opt.value);
                    }

                    lossy = true;
                    break;
                case "LOSSLESS":
                    if (lossy != null) {
                        throw new CompilationException("Duplicate LOSSLESS definition", opt.value);
                    }

                    lossy = false;
                    break;
                case "INITSIZE":
                case "INIT_SIZE":
                    if (initSize != null) {
                        throw new CompilationException("Duplicate INITSIZE definition", opt.value);
                    }

                    initSize = getIntegerValue(opt.value);
                    break;
                case "MAXSIZE":
                case "MAX_SIZE":
                    if (maxSize != null) {
                        throw new CompilationException("Duplicate MAXSIZE definition", opt.value);
                    }

                    maxSize = getIntegerValue(opt.value);
                    break;
                case "MAXTIME":
                case "MAX_TIME":
                    if (maxTime != null) {
                        throw new CompilationException("Duplicate MAXTIME definition", opt.value);
                    }

                    maxTime = getIntegerValue(opt.value);
                    break;
                default:
                    throw new CompilationException("Unexpected definition " + opt.id + " " + opt.value, opt);
            }
        }

        this.key = key;
        this.description = description;
        this.name = name;

        this.ha = ha;
        this.location = loc;
        this.owner = owner;
        this.df = df;
        this.periodicity = periodicity;
        this.versioning = versioning;
        this.lossy = lossy;
        this.initSize = initSize;
        this.maxSize = maxSize;
        this.maxTime = maxTime;
    }

    public void print(StringBuilder s) {
        s.append("ALTER STREAM ");
        id.print(s);
        s.append(" ");
        // todo: print options
        if (alterations != null) {
            for (ClassDef sa : alterations) {
                s.append(" ");
                sa.print(s);
            }
        }
    }

    public static String getStringValue(Expression expression) {
        if (expression instanceof StringConstant) {
            return ((StringConstant) expression).value;
        }

        throw new CompilationException("Invalid type, string required", expression);
    }

    public static Boolean getBoolValue(Expression expression) {
        if (expression instanceof BooleanConstant) {
            return ((BooleanConstant) expression).value;
        }

        throw new CompilationException("Invalid type, bool required", expression);
    }

    public static Long getIntegerValue(Expression expression) {
        if (expression instanceof IntegerConstant) {
            return ((IntegerConstant) expression).value;
        }

        throw new CompilationException("Invalid type, integer required", expression);
    }
}
