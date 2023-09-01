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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 *
 */
public class QAConstant implements QAccessor {
    protected final JExpr reference;

    //protected final boolean isDotNetNullable = false;

    public QAConstant(JExpr reference) {
        this.reference = reference;
    }

    @Override
    public JExpr read() {
        return reference;
    }

    @Override
    public JStatement write(JExpr arg) {
        throw new UnsupportedOperationException("Write to constant");
    }

    @Override
    public String getFieldName() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public String getFieldDescription() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public JStatement writeNullify(JExpr expr){
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public String getSchemaFieldName () {
        throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public Class getFieldType () {
        throw new UnsupportedOperationException(getClass().getName());
    }
}