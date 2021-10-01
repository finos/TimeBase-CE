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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

public class CallExpressionWithInit extends ComplexExpression {

    private final String name;
    private final Expression[] initArgs;
    private final Expression[] nonInitArgs;

    public CallExpressionWithInit(long location, String name, Expression[] initArgs, Expression[] args) {
        super(location, CallExpression.concat(args, initArgs));
        this.name = name;
        this.initArgs = initArgs;
        this.nonInitArgs = args;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        s.append("{");
        printCommaSepArgs(nonInitArgs.length, args.length, s);
        s.append ("}(");
        printCommaSepArgs (0, nonInitArgs.length, s);
        s.append (")");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CallExpressionWithInit))
            return false;
        return super.equals(obj) && name.equals(((CallExpressionWithInit) obj).name);
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 41 + name.hashCode();
    }

    public String getName() {
        return name;
    }

    public Expression[] getInitArgs() {
        return initArgs;
    }

    public int getInitArgsLength() {
        return initArgs.length;
    }

    public Expression[] getNonInitArgs() {
        return nonInitArgs;
    }

    public int getNonInitArgsLength() {
        return nonInitArgs.length;
    }
}