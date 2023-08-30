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
package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;

/**
 *
 */
public class IllegalTypeCombinationException extends CompilationException {
    public IllegalTypeCombinationException (Expression e, DataType t1, DataType t2) {
        super (
            "Illegal combination of types in " + e +
                ": " + t1.getBaseName () + " is not compatible with " +
                t2.getBaseName (),
            e
        );
    }
}