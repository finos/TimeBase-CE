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
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QBooleanType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataTypeHelper;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OrderRelation;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.QRT;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class StrictEqualityCheckOperation extends BinaryExpression {

    private final boolean positive;
    private final OrderRelation relation;
    private final CompiledExpression<?> leftArg;
    private final CompiledExpression<?> rightArg;
    private final boolean differentTypes;

    public StrictEqualityCheckOperation(boolean positive, CompiledExpression<?> leftArg, CompiledExpression<?> rightArg) {
        super(leftArg, rightArg, TimebaseTypes.getBooleanType(TimebaseTypes.isResultNullable(leftArg.type, rightArg.type)));
        this.positive = positive;
        this.relation = positive ? OrderRelation.EQ : OrderRelation.NEQ;
        this.leftArg = leftArg;
        this.rightArg = rightArg;
        this.differentTypes = DataTypeHelper.isNotEqual(leftArg.type, rightArg.type);
    }

    private JExpr generateOperation(JExpr left, JExpr right) {
        if (isDecimal64(leftArg) || isDecimal64(rightArg)) {
            return CTXT.staticCall(QRT.class, "bpos", CTXT.staticCall(Decimal64Utils.class, relation.getDecimalMethod(), left, right));
        } else if (isVarchar(leftArg) || isVarchar(rightArg)) {
            return CTXT.staticCall(QRT.class, relation.getCharSequenceMethod(), left, right);
        } else {
            return CTXT.staticCall(QRT.class, "bpos", CTXT.binExpr(left, relation.getOperator(), right));
        }
    }

    @Override
    public void generateOperation(QValue left, QValue right, QValue out, JCompoundStatement addTo) {
        if (differentTypes) {
            generateDifferentTypes(left, right, out, addTo);
        } else {
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
    }

    protected void generateDifferentTypes(QValue left, QValue right, QValue out, JCompoundStatement addTo) {
        JExpr globalNullCheck = CTXT.binExpr(
                left.readIsNull(true),
                "&&",
                right.readIsNull(true)
        );
        JExpr trueLiteral = QBooleanType.getLiteral(true);
        JExpr falseLiteral = QBooleanType.getLiteral(false);
        if (positive) {
            addTo.add(out.write(CTXT.condExpr(globalNullCheck, trueLiteral, falseLiteral)));
        } else {
            addTo.add(out.write(CTXT.condExpr(globalNullCheck, falseLiteral, trueLiteral)));
        }
    }

    @Override
    public String getOperator() {
        return relation.getOperator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StrictEqualityCheckOperation that = (StrictEqualityCheckOperation) o;

        if (positive != that.positive) return false;
        if (differentTypes != that.differentTypes) return false;
        return relation == that.relation;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (positive ? 1 : 0);
        result = 31 * result + relation.hashCode();
        result = 31 * result + (differentTypes ? 1 : 0);
        return result;
    }
}