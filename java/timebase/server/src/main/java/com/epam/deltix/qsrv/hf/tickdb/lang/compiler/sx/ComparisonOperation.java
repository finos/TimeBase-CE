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
import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QArrayValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.NumericType;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalTypeCombinationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OrderRelation;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.RelationExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.QRT;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JLocalVariable;
import com.epam.deltix.util.jcg.JStatement;

import java.lang.reflect.Modifier;

import static com.epam.deltix.qsrv.hf.pub.md.StandardTypes.isVarcharOrVarcharArray;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class ComparisonOperation extends BinaryExpression {

    private final OrderRelation relation;
    private final CompiledExpression<?> leftArg;
    private final CompiledExpression<?> rightArg;
    private final NumericType leftNumericType;
    private final NumericType rightNumericType;
    private final NumericType comparisonType;

    public ComparisonOperation(OrderRelation relation, CompiledExpression<?> leftArg, CompiledExpression<?> rightArg) {
        super(leftArg, rightArg, resultType(leftArg.type, rightArg.type));
        this.relation = relation;
        this.leftArg = leftArg;
        this.rightArg = rightArg;
        this.leftNumericType = NumericType.forType(leftArg.type);
        this.rightNumericType = NumericType.forType(rightArg.type);
        this.comparisonType = NumericType.resultType(leftNumericType, rightNumericType);
    }

    @Override
    public String getOperator() {
        return relation.getOperator();
    }

    public OrderRelation getRelation() {
        return relation;
    }

    public CompiledExpression<?> getLeftArg() {
        return leftArg;
    }

    public CompiledExpression<?> getRightArg() {
        return rightArg;
    }

    public NumericType getLeftNumericType() {
        return leftNumericType;
    }

    public NumericType getRightNumericType() {
        return rightNumericType;
    }

    public NumericType getComparisonType() {
        return comparisonType;
    }

    private JExpr operation(JExpr left, JExpr right) {
        if (isNumerical()) {
            JExpr leftE = comparisonType.read(left, leftNumericType);
            JExpr rightE = comparisonType.read(right, rightNumericType);
            if (comparisonType == NumericType.Decimal64) {
                return CTXT.staticCall(QRT.class, "bpos", CTXT.staticCall(Decimal64Utils.class, relation.getDecimalMethod(), leftE, rightE));
            } else {
                return CTXT.staticCall(QRT.class, "bpos", CTXT.binExpr(leftE, relation.getOperator(), rightE));
            }
        } else if (isVarcharOrVarcharArray(leftArg.type) && isVarcharOrVarcharArray(rightArg.type)) {
            return CTXT.staticCall(QRT.class, relation.getCharSequenceMethod(), left, right);
        } else {
            return CTXT.staticCall(QRT.class, "bpos", CTXT.binExpr(left, relation.getOperator(), right));
        }
    }

    @Override
    public void generateOperation(QValue left, QValue right, QValue out, JCompoundStatement addTo) {

        boolean isLeftArray = leftArg.type instanceof ArrayDataType;
        boolean isRightArray = rightArg.type instanceof ArrayDataType;

        if (isLeftArray && isRightArray) {
            generateOperation((QArrayValue) left, (QArrayValue) right, (QArrayValue) out, addTo);
        } else if (isLeftArray) {
            generateOperation((QArrayValue) left, right, (QArrayValue) out, addTo);
        } else if (isRightArray) {
            generateOperation(left, (QArrayValue) right, (QArrayValue) out, addTo);
        } else {
            JExpr leftE = left.read();
            JExpr rightE = right.read();
            JExpr e = operation(leftE, rightE);
            JStatement s = out.write(e);
            JExpr globalNullCheck = CTXT.binExpr(
                    left.readIsNull(true),
                    "||",
                    right.readIsNull(true)
            );
            addTo.add(CTXT.ifStmt(globalNullCheck, out.writeNull(), s));
        }
    }

    public void generateOperation(QArrayValue left, QArrayValue right, QArrayValue out, JCompoundStatement addTo) {
        JExpr resultList = out.read();
        JExpr size = QArraysHelper.minSize(left, right);
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", size);
        statement.add(out.setInstance());
        statement.add(resultList.call("setSize", n));
        QType<?> leftType = left.getElementType();
        QType<?> rightType = right.getElementType();
        QType<?> resultType = out.getElementType();
        JExpr nullCheck = CTXT.binExpr(
                leftType.checkNull(left.getElement(i), true),
                "||",
                rightType.checkNull(right.getElement(i), true)
        );
        JExpr setNull = resultList.call("set", i, resultType.getNullLiteral());
        JExpr setValue = resultList.call("set", i, operation(left.getElement(i), right.getElement(i)));
        JCompoundStatement forBody = CTXT.compStmt();
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

    public void generateOperation(QArrayValue left, QValue right, QArrayValue out, JCompoundStatement addTo) {
        JExpr size = left.size();
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", size);
        statement.add(out.setInstance());
        JExpr resultList = out.read();
        statement.add(resultList.call("setSize", n));
        QType<?> leftType = left.getElementType();
        QType<?> resultType = out.getElementType();
        JExpr nullCheck = leftType.checkNull(left.getElement(i), true);
        JExpr setNull = resultList.call("set", i, resultType.getNullLiteral());
        JExpr setValue = resultList.call("set", i, operation(left.getElement(i), right.read()));
        JCompoundStatement forBody = CTXT.compStmt();
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

    public void generateOperation(QValue left, QArrayValue right, QArrayValue out, JCompoundStatement addTo) {
        JExpr size = right.size();
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", size);
        statement.add(out.setInstance());
        JExpr resultList = out.read();
        statement.add(resultList.call("setSize", n));
        QType<?> rightType = right.getElementType();
        QType<?> resultType = out.getElementType();
        JExpr nullCheck = rightType.checkNull(right.getElement(i), true);
        JExpr setNull = resultList.call("set", i, resultType.getNullLiteral());
        JExpr setValue = resultList.call("set", i, operation(left.read(), right.getElement(i)));
        JCompoundStatement forBody = CTXT.compStmt();
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

    public static DataType resultType(DataType type1, DataType type2) {
        boolean isResultNullable = StandardTypes.isResultNullable(type1, type2);
        boolean returnArray = false;
        if (type1 instanceof ArrayDataType) {
            returnArray = true;
            type1 = ((ArrayDataType) type1).getElementDataType();
        }
        if (type2 instanceof ArrayDataType) {
            returnArray = true;
            type2 = ((ArrayDataType) type2).getElementDataType();
        }
        boolean isElementNullable = StandardTypes.isResultNullable(type1, type2);
        return returnArray ? StandardTypes.getBooleanArrayType(isResultNullable, isElementNullable) : StandardTypes.getBooleanType(isResultNullable);
    }

    private boolean isNumerical() {
        return comparisonType != null;
    }

    public static void validate(RelationExpression expression, CompiledExpression<?> left, CompiledExpression<?> right)
            throws IllegalTypeCombinationException {
        boolean isNumericLeft = NumericType.isNumericOrNumericArray(left.type);
        boolean isNumericRight = NumericType.isNumericOrNumericArray(right.type);
        if (isNumericLeft || isNumericRight) {
            if (!(isNumericLeft && isNumericRight))
                throw new IllegalTypeCombinationException(expression, left.type, right.type);
        } else if (left.type.getClass() != right.type.getClass()) {
            throw new IllegalTypeCombinationException(expression, left.type, right.type);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ComparisonOperation operation = (ComparisonOperation) o;

        if (relation != operation.relation) return false;
        if (leftNumericType != operation.leftNumericType) return false;
        if (rightNumericType != operation.rightNumericType) return false;
        return comparisonType == operation.comparisonType;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + relation.hashCode();
        result = 31 * result + (leftNumericType != null ? leftNumericType.hashCode() : 0);
        result = 31 * result + (rightNumericType != null ? rightNumericType.hashCode() : 0);
        result = 31 * result + (comparisonType != null ? comparisonType.hashCode() : 0);
        return result;
    }
}