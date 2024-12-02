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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.*;

/**
 *
 */
abstract class MessageSelectorGenerator extends SelectorGenerator {

    public MessageSelectorGenerator(JClass globalClass, EvalGenerator evalGenerator, SourceClassMap sourceClassMap, JExpr inVar) {
        super(globalClass, evalGenerator, sourceClassMap, inVar);
    }

    @Override
    protected void genInit(JCompoundStatement addTo) {
        addTo.add(
            inVar.call(
                "setBytes",
                evalGenerator.inMsg.field("data"),
                evalGenerator.inMsg.field("offset"),
                evalGenerator.inMsg.field("length")
            )
        );
    }

    @Override
    protected boolean isSkipFieldDecoding(FieldSelectorInfo fsi) {
        return fsi.cache == null || (fsi.fieldSelector == null && !fsi.usedAsBase);
    }

}
