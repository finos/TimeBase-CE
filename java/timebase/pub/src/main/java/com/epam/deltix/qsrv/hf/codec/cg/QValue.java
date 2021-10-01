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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JCompoundStatement;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;

/**
 * Represents a value bound to a constant, variable or object field.
 * <p>
 * Notice that a value can be kept in more then one java variable 
 * (see {@link QAVariable} for example).
 * </p>
 */
public class QValue<T extends QType> {
    public final T type;

    public QValue(T type) {
        this.type = type;
    }

    public JExpr readIsNull(boolean eq) {
        throw new UnsupportedOperationException(
            "Not implemented for  " + getClass().getSimpleName()
        );
    }

    public JStatement writeNull() {
        throw new UnsupportedOperationException(
            "Not implemented for " + getClass().getSimpleName()
        );
    }

    public JStatement  writeIsNull (JExpr arg) {
        throw new UnsupportedOperationException(
            "Not implemented for  " + getClass().getSimpleName()
        );
    }

    public JExpr       read () {
        throw new UnsupportedOperationException(
            "Not implemented for  " + getClass().getSimpleName()
        );
    }

    public JStatement  write (JExpr arg) {
        throw new UnsupportedOperationException(
            "Not implemented for  " + getClass().getSimpleName()
        );
    }

    public void decode(JExpr input, JCompoundStatement addTo) {
        throw notImplemented();
    }

    public void decodeRelative(
        JExpr input,
        QValue base,
        JExpr isBaseNull,
        JCompoundStatement addTo
    )
    {
        throw notImplemented();
    }

    public void encode(JExpr output, JCompoundStatement addTo) {
        throw notImplemented();
    }

    protected UnsupportedOperationException notImplemented() {
        return new UnsupportedOperationException(
            "Not implemented for " + getClass().getSimpleName()
        );
    }
}