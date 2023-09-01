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
import com.epam.deltix.util.jcg.JSwitchStatement;

/**
 *
 */
class SelectorGeneratorForRootObjects extends SelectorGenerator {

    protected final JClass filterClass;

    public SelectorGeneratorForRootObjects(JClass globalClass, EvalGenerator evalGenerator,
                                           SourceClassMap sourceClassMap, JClass filterClass, JExpr inVar)
    {
        super(globalClass, evalGenerator, sourceClassMap, inVar);

        this.filterClass = filterClass;
    }

    @Override
    protected JExpr getTypeIdxExpr() {
        return filterClass.callSuperMethod("getInputTypeIndex");
    }

    @Override
    protected void generateInit(JCompoundStatement addTo) {
        // generate nulls
        sourceClassMap.forEachValue((value) -> {
            addTo.add(value.writeNull());
        });

        super.generateInit(addTo);
    }

    protected void addDefaultSwitch(JSwitchStatement switchStatement) {
    }

}