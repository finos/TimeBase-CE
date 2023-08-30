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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QArrayValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataTypeHelper;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.NumericType;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalTypeCombinationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ArithmeticExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ArithmeticFunction;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JLocalVariable;
import com.epam.deltix.util.jcg.JStatement;

import java.lang.reflect.Modifier;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class ArithmeticOperation extends BinaryExpression {

    private final ArithmeticFunction function;
    private final boolean isLeftArray;
    private final boolean isRightArray;
    private final NumericType leftType;
    private final NumericType rightType;
    private final NumericType result;

    public ArithmeticOperation(ArithmeticFunction function, CompiledExpression<?> left, CompiledExpression<?> right) {
        super(left, right, NumericType.resultType(left.type, right.type));
        this.function = function;
        this.isLeftArray = NumericType.isNumericArrayType(left.type);
        this.isRightArray = NumericType.isNumericArrayType(right.type);
        this.leftType = NumericType.forType(args[0].type);
        this.rightType = NumericType.forType(args[1].type);
        this.result = NumericType.resultType(leftType, rightType);
    }

    @Override
    public void generateOperation(QValue left, QValue right, QValue out, JCompoundStatement addTo) {
        if (isLeftArray && isRightArray) {
            generateForArrays((QArrayValue) left, (QArrayValue) right, (QArrayValue) out, addTo);
        } else if (isLeftArray) {
            generateLeftArray((QArrayValue) left, right, out, addTo);
        } else if (isRightArray) {
            generateRightArray(left, (QArrayValue) right, out, addTo);
        } else {
            JExpr e = operation(left, right);
            JStatement s = out.write(e);
            s = QType.wrapWithNullCheck(s, left, out);
            s = QType.wrapWithNullCheck(s, right, out);
            addTo.add(s);
        }
    }

    private JExpr operation(JExpr left, JExpr right) {
        JExpr leftE = result.read(left, leftType);
        JExpr rightE = result.read(right, rightType);
        return result == NumericType.Decimal64 ? CTXT.staticCall(Decimal64Utils.class, function.getDecimalMethod(), leftE, rightE) :
                result.cast(CTXT.binExpr(leftE, function.getOperator(), rightE));
    }

    private JExpr operation(QValue left, QValue right) {
        JExpr leftE = result.read(left, leftType);
        JExpr rightE = result.read(right, rightType);
        return result == NumericType.Decimal64 ? CTXT.staticCall(Decimal64Utils.class, function.getDecimalMethod(), leftE, rightE) :
                result.cast(CTXT.binExpr(leftE, function.getOperator(), rightE));
    }

    private void generateForArrays(QArrayValue left, QArrayValue right, QArrayValue out, JCompoundStatement addTo) {
        JExpr resultList = out.read();
        JExpr minSize = QArraysHelper.minSize(left, right);
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", minSize);
        statement.add(out.setInstance());
        statement.add(resultList.call("setSize", n));
        JCompoundStatement forBody = CTXT.compStmt();
        JExpr nullCheck = CTXT.binExpr(
                leftType.checkNull(left.getElement(i)),
                "||",
                rightType.checkNull(right.getElement(i))
        );
        JExpr setValue = resultList.call("set", i, operation(left.getElement(i), right.getElement(i)));
        JExpr setNull = resultList.call("set", i, result.nullExpression());
        forBody.add(CTXT.ifStmt(nullCheck, setNull.asStmt(), setValue.asStmt()));
        statement.add(CTXT.forStmt(i.assignExpr(CTXT.intLiteral(0)), CTXT.binExpr(i, "<", n), i.getAndInc(), forBody));
        statement.add(out.setChanged());
        JExpr globalNullCheck = CTXT.binExpr(
                left.readIsNull(true),
                "||",
                right.readIsNull(true)
        );
        addTo.add(CTXT.ifStmt(globalNullCheck, out.writeNull(), statement));
    }

    private void generateLeftArray(QArrayValue left, QValue right, QValue out, JCompoundStatement addTo) {
        QArrayValue outValue = (QArrayValue) out;
        JExpr rightValue = right.read();
        JExpr resultList = outValue.read();
        JExpr size = left.size();
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", size);
        statement.add(outValue.setInstance());
        statement.add(resultList.call("setSize", n));
        JCompoundStatement forBody = CTXT.compStmt();
        JExpr nullCheck = leftType.checkNull(left.getElement(i));
        JExpr setValue = resultList.call("set", i, operation(left.getElement(i), rightValue));
        JExpr setNull = resultList.call("set", i, result.nullExpression());
        forBody.add(CTXT.ifStmt(nullCheck, setNull.asStmt(), setValue.asStmt()));
        statement.add(CTXT.forStmt(i.assignExpr(CTXT.intLiteral(0)), CTXT.binExpr(i, "<", n), i.getAndInc(), forBody));
        statement.add(outValue.setChanged());
        JExpr globalNullCheck = CTXT.binExpr(
                left.readIsNull(true),
                "||",
                right.readIsNull(true)
        );
        addTo.add(CTXT.ifStmt(globalNullCheck, out.writeNull(), statement));
    }

    private void generateRightArray(QValue left, QArrayValue right, QValue out, JCompoundStatement addTo) {
        QArrayValue outValue = (QArrayValue) out;
        JExpr leftValue = left.read();
        JExpr resultList = outValue.read();
        JExpr size = right.size();
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", size);
        statement.add(outValue.setInstance());
        statement.add(resultList.call("setSize", n));
        JCompoundStatement forBody = CTXT.compStmt();
        JExpr nullCheck = rightType.checkNull(right.getElement(i));
        JExpr setValue = resultList.call("set", i, operation(leftValue, right.getElement(i)));
        JExpr setNull = resultList.call("set", i, result.nullExpression());
        forBody.add(CTXT.ifStmt(nullCheck, setNull.asStmt(), setValue.asStmt()));
        forBody.add(setValue);
        statement.add(CTXT.forStmt(i.assignExpr(CTXT.intLiteral(0)), CTXT.binExpr(i, "<", n), i.getAndInc(), forBody));
        statement.add(outValue.setChanged());
        JExpr globalNullCheck = CTXT.binExpr(
                left.readIsNull(true),
                "||",
                right.readIsNull(true)
        );
        addTo.add(CTXT.ifStmt(globalNullCheck, out.writeNull(), statement));
    }

    @Override
    public String getOperator() {
        return function.getOperator();
    }

    public static void validateArgs(ArithmeticExpression expression, CompiledExpression<?> left, CompiledExpression<?> right) throws IllegalTypeCombinationException {
        if (expression.function == ArithmeticFunction.MOD) {
            if (!NumericType.isInteger(left.type) || !NumericType.isInteger(right.type)) {
                throw new IllegalTypeCombinationException(expression, left.type, right.type);
            }
        } else if (DataTypeHelper.isTimestampAndInteger(left.type, right.type)) {
            if (expression.function != ArithmeticFunction.ADD && expression.function != ArithmeticFunction.SUB) {
                throw new IllegalTypeCombinationException(expression, left.type, right.type);
            }
        } else if (DataTypeHelper.isTimestampAndTimestamp(left.type, right.type)) {
            if (expression.function != ArithmeticFunction.SUB) {
                throw new IllegalTypeCombinationException(expression, left.type, right.type);
            }
        } else {
            if (!NumericType.isNumericOrNumericArray(left.type) || !NumericType.isNumericOrNumericArray(right.type)) {
                throw new IllegalTypeCombinationException(expression, left.type, right.type);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ArithmeticOperation)) {
            return false;
        }
        ArithmeticOperation other = (ArithmeticOperation) obj;
        return super.equals(obj)
                && function == other.function
                && isLeftArray == other.isLeftArray
                && isRightArray == other.isRightArray
                && leftType == other.leftType
                && rightType == other.rightType
                && result == other.result;
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + function.hashCode();
        result1 = 31 * result1 + (isLeftArray ? 1 : 0);
        result1 = 31 * result1 + (isRightArray ? 1 : 0);
        result1 = 31 * result1 + leftType.hashCode();
        result1 = 31 * result1 + rightType.hashCode();
        result1 = 31 * result1 + result.hashCode();
        return result1;
    }
}