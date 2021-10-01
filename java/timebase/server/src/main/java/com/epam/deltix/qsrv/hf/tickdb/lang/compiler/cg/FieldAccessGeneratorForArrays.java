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

import com.epam.deltix.util.jcg.*;

import java.lang.reflect.Modifier;
import java.util.List;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
class FieldAccessGeneratorForArrays extends FieldAccessGenerator {

    protected final QArrayValue parent;
    private final boolean fetchNulls;

    public FieldAccessGeneratorForArrays(JClass globalClass, EvalGenerator evalGenerator,
                                         SourceClassMap sourceClassMap, QValue parent,
                                         boolean fetchNulls
    ) {
        super(globalClass, evalGenerator, sourceClassMap);

        this.parent = (QArrayValue) parent;
        this.fetchNulls = fetchNulls;
    }

    @Override
    protected JExpr getTypeIdxExpr() {
        return parent.getElement().call("typeId");
    }

    protected void addDefaultSwitch(JSwitchStatement switchStatement) {
        switchStatement.addDefaultLabel();

        if (fetchNulls) {
            // generate nulls
            sourceClassMap.forEachFieldSelector((selectorInfo) -> {
                if (selectorInfo.fieldAccessor != null && selectorInfo.fieldAccessor.fetchNulls) {
                    switchStatement.add(writeNullStatement(selectorInfo.cache));
                }
            });
        }
    }

    protected void addNullValues(JCompoundStatement addTo, List<QValue> written) {
        if (written != null) {
            if (fetchNulls) {
                sourceClassMap.forEachFieldSelector((selectorInfo) -> {
                    if (selectorInfo.fieldAccessor != null && selectorInfo.fieldAccessor.fetchNulls) {
                        if (!written.contains(selectorInfo.cache)) {
                            addTo.add(writeNullStatement(selectorInfo.cache));
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void generateInit(JCompoundStatement addTo) {
        // generate nulls
        sourceClassMap.forEachValue((value) -> {
            if (value instanceof QArrayValue) {
                evalGenerator.addTo.add(((QArrayValue) value).setNull());
            }
        });

        // state.v1.next();
        addTo.add(CTXT.ifStmt(parent.next().not(), CTXT.continueStmt()));

        // in.setBytes(state.instance.bytes(), state.instance.offset(), state.instance.length());
        addTo.add(
            globalClass.getVar("in").access().call(
                "setBytes",
                parent.getElement().call("bytes"),
                parent.getElement().call("offset"),
                parent.getElement().call("length")
            )
        );
    }

    @Override
    protected JStatement wrapSelectors(JStatement selectors) {
        return CTXT.ifStmt(getConditionExpr(), generateForEach(selectors));
    }

    private JStatement generateForEach(JStatement selectors) {
        JCompoundStatement statements = CTXT.compStmt();

        final JLocalVariable len = statements.addVar(Modifier.FINAL, int.class, "len", parent.startRead());
        final JLocalVariable ii = statements.addVar(0, int.class, "ii");
        statements.add(
            CTXT.forStmt(
                ii.assignExpr(CTXT.intLiteral(0)),
                CTXT.binExpr(ii, "<", len),
                ii.getAndInc(),
                selectors
            )
        );

        return statements;
    }

    private JExpr getConditionExpr() {
        return parent.isNull().not();
    }

    protected JStatement writeStatement(QValue target) {
        return ((QArrayValue) target).decodeElement(inVar);
    }

    protected JStatement writeNullStatement(QValue target) {
        return ((QArrayValue) target).addNull();
    }

}