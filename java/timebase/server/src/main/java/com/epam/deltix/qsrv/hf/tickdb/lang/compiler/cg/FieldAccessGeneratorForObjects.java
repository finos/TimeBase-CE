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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.JClass;
import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

/**
 *
 */
class FieldAccessGeneratorForObjects extends FieldAccessGenerator {

    protected final QValue parent;

    public FieldAccessGeneratorForObjects(JClass globalClass, EvalGenerator evalGenerator, SourceClassMap sourceClassMap, QValue parent) {
        super(globalClass, evalGenerator, sourceClassMap);

        this.parent = parent;
    }

    protected boolean setObjectTypes() {
        return true;
    }

    @Override
    protected JExpr getTypeIdxExpr() {
        return parent.read().call("typeId");
    }

    @Override
    protected void generateInit(JCompoundStatement addTo) {
        // generate nulls
        sourceClassMap.forEachValue((value) -> {
            evalGenerator.addTo.add(value.writeNull());
        });

        // in.setBytes(state.instance.bytes(), state.instance.offset(), state.instance.length());
        addTo.add(
            inVar.call(
                "setBytes",
                parent.read().call("bytes"),
                parent.read().call("offset"),
                parent.read().call("length")
            )
        );
    }

    protected JStatement wrapSelectors(JStatement selectors) {
        return CTXT.ifStmt(getConditionExpr(), selectors);
    }

    private JExpr getConditionExpr() {
        // if (!objectValue.isEmpty())
        return parent.readIsNull(false);
    }
}