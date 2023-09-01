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
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QArrayValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QBooleanType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataTypeHelper;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.NumericType;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalTypeCombinationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OrderRelation;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.QRT;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JLocalVariable;
import com.epam.deltix.util.jcg.JStatement;

import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class EqualityCheckOperation extends BinaryExpression {

    private final boolean positive;
    private final OrderRelation relation;
    private final CompiledExpression<?> leftArg;
    private final CompiledExpression<?> rightArg;

    public EqualityCheckOperation(boolean positive, CompiledExpression<?> leftArg, CompiledExpression<?> rightArg) {
        super(leftArg, rightArg, ComparisonOperation.resultType(leftArg.type, rightArg.type));
        this.positive = positive;
        this.relation = positive ? OrderRelation.EQ : OrderRelation.NEQ;
        this.leftArg = leftArg;
        this.rightArg = rightArg;
    }

    private JExpr generateOperation(JExpr left, JExpr right) {
        if (DataTypeHelper.isElementDecimal64(leftArg.type) || DataTypeHelper.isElementDecimal64(rightArg.type)) {
            return CTXT.staticCall(QRT.class, "bpos", CTXT.staticCall(Decimal64Utils.class, relation.getDecimalMethod(), left, right));
        } else if (DataTypeHelper.isElementVarchar(leftArg.type) || DataTypeHelper.isElementVarchar(rightArg.type)) {
            return CTXT.staticCall(QRT.class, relation.getCharSequenceMethod(), left, right);
        } else {
            return CTXT.staticCall(QRT.class, "bpos", CTXT.binExpr(left, relation.getOperator(), right));
        }
    }

    @Override
    public void generateOperation(QValue left, QValue right, QValue out, JCompoundStatement addTo) {
        if (left instanceof QArrayValue && right instanceof QArrayValue) {
            generateOperation((QArrayValue) left, (QArrayValue) right, (QArrayValue) out, addTo);
        } else if (left instanceof QArrayValue) {
            generateOperation((QArrayValue) left, right, (QArrayValue) out, addTo);
        } else if (right instanceof QArrayValue) {
            generateOperation(left, (QArrayValue) right, (QArrayValue) out, addTo);
        } else {
            generateSimple(left, right, out, addTo);
        }
    }

    public void generateOperation(QArrayValue left, QArrayValue right, QArrayValue out, JCompoundStatement addTo) {
        JExpr resultList = out.read();
        JExpr minSize = QArraysHelper.minSize(left, right);
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", minSize);
        statement.add(out.setInstance());
        statement.add(resultList.call("setSize", n));
        JCompoundStatement forBody = CTXT.compStmt();
        JExpr nullCheck = CTXT.binExpr(
                left.getElementType().checkNull(left.getElement(i), true),
                "||",
                right.getElementType().checkNull(right.getElement(i), true)
        );
        JExpr setValue = resultList.call("set", i, generateOperation(left.getElement(i), right.getElement(i)));
        JExpr setNull = resultList.call("set", i, out.getElementType().getNullLiteral());
        forBody.add(CTXT.ifStmt(nullCheck, setNull.asStmt(), setValue.asStmt()));
        statement.add(CTXT.forStmt(i.assignExpr(CTXT.intLiteral(0)), CTXT.binExpr(i, "<", n), i.getAndInc(), forBody));
        statement.add(out.setChanged());
        JExpr globalNullCheck = CTXT.binExpr(
                left.readIsNull(true),
                "||",
                right.readIsNull(true)
        );
        addTo.add(CTXT.ifStmt(globalNullCheck, out.writeEmpty(), statement));
    }

    public void generateOperation(QArrayValue left, QValue right, QArrayValue out, JCompoundStatement addTo) {
        JExpr resultList = out.read();
        JExpr minSize = left.size();
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", minSize);
        statement.add(out.setInstance());
        statement.add(resultList.call("setSize", n));
        JCompoundStatement forBody = CTXT.compStmt();
        JExpr nullCheck = left.getElementType().checkNull(left.getElement(i), true);
        JExpr setValue = resultList.call("set", i, generateOperation(left.getElement(i), right.read()));
        JExpr setNull = resultList.call("set", i, out.getElementType().getNullLiteral());
        forBody.add(CTXT.ifStmt(nullCheck, setNull.asStmt(), setValue.asStmt()));
        statement.add(CTXT.forStmt(i.assignExpr(CTXT.intLiteral(0)), CTXT.binExpr(i, "<", n), i.getAndInc(), forBody));
        statement.add(out.setChanged());
        JExpr globalNullCheck = left.readIsNull(true);
        addTo.add(CTXT.ifStmt(globalNullCheck, out.writeEmpty(), statement));
    }

    public void generateOperation(QValue left, QArrayValue right, QArrayValue out, JCompoundStatement addTo) {
        JExpr resultList = out.read();
        JExpr minSize = right.size();
        JCompoundStatement statement = CTXT.compStmt();
        JLocalVariable i = statement.addVar(0, int.class, "i");
        JLocalVariable n = statement.addVar(Modifier.FINAL, int.class, "n", minSize);
        statement.add(out.setInstance());
        statement.add(resultList.call("setSize", n));
        JCompoundStatement forBody = CTXT.compStmt();
        JExpr nullCheck = right.getElementType().checkNull(right.getElement(i), true);
        JExpr setValue = resultList.call("set", i, generateOperation(left.read(), right.getElement(i)));
        JExpr setNull = resultList.call("set", i, out.getElementType().getNullLiteral());
        forBody.add(CTXT.ifStmt(nullCheck, setNull.asStmt(), setValue.asStmt()));
        statement.add(CTXT.forStmt(i.assignExpr(CTXT.intLiteral(0)), CTXT.binExpr(i, "<", n), i.getAndInc(), forBody));
        statement.add(out.setChanged());
        JExpr globalNullCheck = right.readIsNull(true);
        addTo.add(CTXT.ifStmt(globalNullCheck, out.writeEmpty(), statement));
    }

    public void generateSimple(QValue left, QValue right, QValue out, JCompoundStatement addTo) {
        JExpr leftE = left.read();
        JExpr rightE = right.read();

        JExpr e = generateOperation(leftE, rightE);

        JStatement s = out.write(e);

        boolean leftIsNullable = left.type.isNullable();
        boolean rightIsNullable = right.type.isNullable();
        JExpr negative = QBooleanType.getLiteral(!positive);

        if (leftIsNullable) {
            if (rightIsNullable) {
                s = CTXT.ifStmt(
                        left.readIsNull(true),
                        out.write(QBooleanType.cleanToNullable(right.readIsNull(positive))),
                        CTXT.ifStmt(
                                right.readIsNull(true), // but left is not null
                                out.write(negative),
                                s
                        )
                );
            } else {
                s = CTXT.ifStmt(
                        left.readIsNull(true),
                        out.write(negative),
                        s
                );
            }
        } else if (rightIsNullable) {
            s = CTXT.ifStmt(
                    right.readIsNull(true),
                    out.write(negative),
                    s
            );
        }
        addTo.add(s);
    }

    @Override
    public String getOperator() {
        return relation.getOperator();
    }

    public static void validate(Expression expression, DataType dt1, DataType dt2) {
        if (both(NumericType::isNumericOrNumericArray, dt1, dt2) ||
                both(TimebaseTypes::isBooleanOrBooleanArray, dt1, dt2) ||
                bothEqualEnums(dt1, dt2) ||
                both(TimebaseTypes::isVarcharOrVarcharArray, dt1, dt2) ||
                both(TimebaseTypes::isDateTimeOrDateTimeArray, dt1, dt2) ||
                both(TimebaseTypes::isTimeOfDayOrTimeOfDayArray, dt1, dt2) ||
                both(TimebaseTypes::isCharOrCharArray, dt1, dt2))
            return;
        throw new IllegalTypeCombinationException(expression, dt1, dt2);
    }

    private static boolean bothEqualEnums(DataType dt1, DataType dt2) {
        return TimebaseTypes.isEnumOrEnumArray(dt1) && TimebaseTypes.isEnumOrEnumArray(dt2) &&
                TimebaseTypes.extractEnumClassDescriptor(dt1).getGuid().equals(TimebaseTypes.extractEnumClassDescriptor(dt2).getGuid());
    }

    private static boolean both(Predicate<DataType> predicate, DataType dt1, DataType dt2) {
        return predicate.test(dt1) && predicate.test(dt2);
    }

    public OrderRelation getRelation() {
        return relation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EqualityCheckOperation that = (EqualityCheckOperation) o;

        if (positive != that.positive) return false;
        return relation == that.relation;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (positive ? 1 : 0);
        result = 31 * result + relation.hashCode();
        return result;
    }
}