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

import com.epam.deltix.util.jcg.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public final class QByteSkipContext {
    private int                         bytesSkipped = 0;
    private final JExpr                 input;
    final JCompoundStatement            addTo;
    private final String                function; 

    public QByteSkipContext (JExpr input, JCompoundStatement addTo) {
        this(true, input, addTo);
    }

    public QByteSkipContext (boolean isInput, JExpr memoryData, JCompoundStatement addTo) {
        this.input = memoryData;
        this.addTo = addTo;
        this.function = isInput ? "skipBytes": "skip";
    }

    public void                 skipBytes (int n) {
        bytesSkipped += n;
    }

    public void                 flush () {
        if (bytesSkipped == 0)
            return;

        addTo.add (input.call (function, CTXT.intLiteral (bytesSkipped)));
        bytesSkipped = 0;
    }

    public JStatement                 flush2 () {
        if (bytesSkipped == 0)
            return null;

        final JStatement stmt = input.call(function, CTXT.intLiteral(bytesSkipped)).asStmt();
        bytesSkipped = 0;
        return stmt;
    }
}