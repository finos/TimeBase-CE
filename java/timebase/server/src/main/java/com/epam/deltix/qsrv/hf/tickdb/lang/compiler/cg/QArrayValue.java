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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QArrayValue extends QValue {
    private final JExpr variable;
    private final QArrayType arrayType;

    public QArrayValue(QType<ArrayDataType> type, JExpr variable) {
        super(type);

        this.variable = variable;
        this.arrayType = (QArrayType) type;
    }

    public void skip(JExpr input, JCompoundStatement addTo) {
    }

    @Override
    public JExpr read() {
        return variable.call("get");
    }

    public JExpr readTyped() {
        return variable.call("getTyped");
    }

    public JExpr writeTyped() {
        return variable.call("writeTyped");
    }

    public JExpr getPool() {
        return variable.call("getPool");
    }

    public JExpr setChanged() {
        return variable.call("setChanged");
    }

    public JExpr setTypedChanged() {
        return variable.call("setTypedChanged");
    }

    public JExpr setInstance() {
        return variable.call("setInstance");
    }

    public JExpr size() {
        return variable.call("get").call("size");
    }

    public JExpr variable() {
        return variable;
    }

    public JExpr setList(JExpr otherList) {
        return variable.call("setList", otherList);
    }

    // write same as add element
    @Override
    public JStatement writeNull() {
        return setNull().asStmt();
    }

    public JStatement writeEmpty() {
        return setEmpty().asStmt();
    }

    @Override
    public JStatement write(JExpr arg) {
        return variable.call("add", arg).asStmt();
    }

    public JStatement addNull() {
        return write(arrayType.getElementNullLiteral());
    }

    public JStatement decodeElement(JExpr arg) {
        return arrayType.decodeElement(arg, this);
    }

    public JExpr startRead() {
        return variable.call("startRead");
    }

    public JExpr next() {
        return variable.call("next");
    }

    public JExpr getElement() {
        return variable.call("getElement");
    }

    public JExpr isNull() {
        return variable.call("isNull");
    }

    public JExpr setNull() {
        return variable.call("setNull");
    }

    public JExpr setEmpty() {
        return variable.call("setEmpty");
    }

    public QType<?> getElementType() {
        return arrayType.getElementType();
    }

    public JExpr getElement(JExpr index) {
        return read().call(arrayType.listGetMethod(), index);
    }

    public JExpr adjustType(int adjustTypeIndex) {
        return variable.call("adjustTypeId", CTXT.intLiteral(adjustTypeIndex));
    }

    public boolean isObjectArray() {
        return arrayType.isObjectArray();
    }

    public JExpr copy(JExpr another) {
        return variable.call("copyFrom", another);
    }

    public boolean hasClasses() {
        return arrayType.hasClasses();
    }

}