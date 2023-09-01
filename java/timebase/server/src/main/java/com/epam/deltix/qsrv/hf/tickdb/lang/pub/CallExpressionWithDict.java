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

import java.util.Map;

public class CallExpressionWithDict extends ComplexExpression {

    private final String name;
    private final Map<String, Expression> dict;
    private final Expression[] nonInitArgs;

    public CallExpressionWithDict(long location, String name, Map<String, Expression> dictionary, Expression[] args) {
        super(location, CallExpression.concat(args, dictionary.values().toArray(new Expression[dictionary.size()])));
        this.name = name;
        this.dict = dictionary;
        this.nonInitArgs = args;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        GrammarUtil.escapeIdentifier(NamedObjectType.FUNCTION, name, s);
        if (dict.isEmpty()) {
            s.append("{}");
        } else {
            s.append("{");
            dict.forEach((k, v) -> {
                s.append(k).append(": ");
                v.print(s);
                s.append(", ");
            });
            s.setLength(s.length() - 2);
            s.append("}");
        }
        s.append ("(");
        printCommaSepArgs (0, nonInitArgs.length, s);
        s.append (")");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CallExpressionWithDict))
            return false;
        return super.equals(obj) && name.equals(((CallExpressionWithDict) obj).name);
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 41 * 41 + name.hashCode() * 41 + dict.hashCode());
    }

    public String getName() {
        return name;
    }

    public Map<String, Expression> getDict() {
        return dict;
    }

    public Expression[] getNonInitArgs() {
        return nonInitArgs;
    }

    public int getNonInitArgsLength() {
        return nonInitArgs.length;
    }
}