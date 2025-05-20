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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.NumericType;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalTypeCombinationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.BitwiseExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.BitwiseFunction;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class BitwiseOperation extends BinaryExpression {

    private final BitwiseFunction function;
    private final NumericType leftType;
    private final NumericType rightType;
    private final NumericType result;

    public BitwiseOperation(BitwiseFunction function, CompiledExpression<?> left, CompiledExpression<?> right) {
        super(left, right, NumericType.resultType(left.type, right.type));
        this.function = function;
        this.leftType = NumericType.forType(args[0].type);
        this.rightType = NumericType.forType(args[1].type);
        this.result = NumericType.resultType(leftType, rightType);
    }

    @Override
    public void generateOperation(QValue left, QValue right, QValue out, JCompoundStatement addTo) {
        JExpr e = operation(left, right);
        JStatement s = out.write(e);
        s = QType.wrapWithNullCheck(s, left, out);
        s = QType.wrapWithNullCheck(s, right, out);
        addTo.add(s);
    }

    private JExpr operation(JExpr left, JExpr right) {
        JExpr leftE = result.read(left, leftType);
        JExpr rightE = result.read(right, rightType);
        return result.cast(CTXT.binExpr(leftE, function.getOperator(), rightE));
    }

    private JExpr operation(QValue left, QValue right) {
        JExpr leftE = result.read(left, leftType);
        JExpr rightE = result.read(right, rightType);
        return result.cast(CTXT.binExpr(leftE, function.getOperator(), rightE));
    }

    @Override
    public String getOperator() {
        return function.getOperator();
    }

    public static void validateArgs(BitwiseExpression expression,
                                    CompiledExpression<?> left, CompiledExpression<?> right) throws IllegalTypeCombinationException {
        if (!(NumericType.isIntegerType(left.type) || isEnumType(left.type)) ||
            !(NumericType.isIntegerType(right.type) || isEnumType(right.type))) {

            throw new IllegalTypeCombinationException(expression, left.type, right.type);
        }
    }

    private static boolean isEnumType(DataType dataType) {
        return dataType instanceof EnumDataType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BitwiseOperation)) {
            return false;
        }
        BitwiseOperation other = (BitwiseOperation) obj;
        return super.equals(obj)
                && function == other.function
                && leftType == other.leftType
                && rightType == other.rightType
                && result == other.result;
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + function.hashCode();
        result1 = 31 * result1 + leftType.hashCode();
        result1 = 31 * result1 + rightType.hashCode();
        result1 = 31 * result1 + result.hashCode();
        return result1;
    }
}
