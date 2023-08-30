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

/**
 *
 */
public class NanCheckExpression extends ComplexExpression {
    public final boolean checkIsNan;

    public NanCheckExpression(long location, Expression arg, boolean checkIsNan) {
        super(location, arg);
        this.checkIsNan = checkIsNan;
    }

    public NanCheckExpression(Expression arg, boolean checkIsNan) {
        super(NO_LOCATION, arg);
        this.checkIsNan = checkIsNan;
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        getArgument().print(outerPriority, s);
        s.append(" IS ");

        if (!checkIsNan) {
            s.append("NOT ");
        }

        s.append("NAN");
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return (
            super.equals(obj) &&
                checkIsNan == ((NanCheckExpression) obj).checkIsNan
        );
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * (checkIsNan ? 23 : 41));
    }
}
