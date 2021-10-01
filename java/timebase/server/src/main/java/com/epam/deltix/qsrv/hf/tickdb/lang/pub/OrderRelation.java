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

/**
 *  Less or greater, possibly equals.
 */
public enum OrderRelation {
    EQ("==", "isEqual", "seq"),
    NEQ("!=", "isNotEqual", "sneq"),
    LT("<", "isLess", "slt"),
    LE("<=", "isLessOrEqual", "sle"),
    GT(">", "isGreater", "sgt"),
    GE(">=", "isGreaterOrEqual", "sge");

    private final String operator;
    private final String decimalMethod;
    private final String charSequenceMethod;

    OrderRelation(final String operator, final String decimalMethod, final String charSequenceMethod) {
        this.operator = operator;
        this.decimalMethod = decimalMethod;
        this.charSequenceMethod = charSequenceMethod;
    }

    public String getOperator() {
        return operator;
    }

    public String getDecimalMethod() {
        return decimalMethod;
    }

    public String getCharSequenceMethod() {
        return charSequenceMethod;
    }
}