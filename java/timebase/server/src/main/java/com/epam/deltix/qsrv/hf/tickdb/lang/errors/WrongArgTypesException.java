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
package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class WrongArgTypesException extends CompilationException {
    private static String    diag (String name, DataType [] types) {
        StringBuilder   sb = new StringBuilder ();

        sb.append ("Function ");
        sb.append (name);
        sb.append (" () may not be applied to (");

        int             n = types.length;

        for (int ii = 0; ii < n; ii++) {
            if (ii > 0)
                sb.append (", ");

            sb.append (types [ii].getBaseName ());
        }

        sb.append (")");

        return (sb.toString ());
    }

    public WrongArgTypesException (CallExpression e, DataType [] types) {
        super (diag (e.name, types), e);
    }
}
