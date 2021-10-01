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

import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.PolyObjectContainer;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JVariable;

import java.util.Arrays;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class QExtendedObjectType extends QObjectType {

    private final Class<?>[] classes;

    public QExtendedObjectType(ClassDataType dt) throws ClassNotFoundException {
        super(dt);
        this.classes = new Class[dt.getDescriptors().length];
        for (int i = 0; i < dt.getDescriptors().length; i++) {
            this.classes[i] = Class.forName(dt.getDescriptors()[i].getName());
        }
    }

    @Override
    public QValue declareValue(String comment, QVariableContainer container, QClassRegistry registry, boolean setNull) {
        JExpr init = CTXT.newExpr(PolyObjectContainer.class, Arrays.stream(classes).map(CTXT::classLiteral).toArray(JExpr[]::new));
        JVariable v = container.addVar(comment, true, PolyObjectContainer.class, init);
        return new QExtendedObjectValue(this, container.access(v));
    }
}