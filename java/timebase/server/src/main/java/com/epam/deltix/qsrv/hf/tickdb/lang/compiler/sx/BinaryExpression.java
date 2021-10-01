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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.util.jcg.JCompoundStatement;

public abstract class BinaryExpression extends CompiledComplexExpression {

    public BinaryExpression(CompiledExpression<?> left, CompiledExpression<?> right, DataType result) {
        super(result, left, right);
    }

    public abstract void generateOperation(QValue left, QValue right, QValue out, JCompoundStatement addTo);

    public abstract String getOperator();

    @Override
    public void print(StringBuilder out) {
        args[0].print(out);
        out.append(" ").append(getOperator()).append(" ");
        args[1].print(out);
    }

    public static boolean isDecimal64(CompiledExpression<?> expresion) {
        return expresion.type instanceof FloatDataType && ((FloatDataType) expresion.type).isDecimal64();
    }

    public static boolean isVarchar(CompiledExpression<?> expression) {
        return expression.type instanceof VarcharDataType;
    }
}