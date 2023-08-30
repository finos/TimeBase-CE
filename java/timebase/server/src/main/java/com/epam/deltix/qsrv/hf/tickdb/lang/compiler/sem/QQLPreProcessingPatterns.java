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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.*;

/**
 *
 */
abstract class QQLPreProcessingPatterns {
    public static boolean       isThis (Expression e) {
        return (e instanceof This);
    }
    
    public static boolean       isLastThis (Expression e) {
        return (isCallThis (e, KEYWORD_LAST));
    }
    
    public static boolean       isFirstThis (Expression e) {
        return (isCallThis (e, KEYWORD_FIRST));
    }
    
    private static boolean      isCallThis (Expression e, String func) {
        if (!(e instanceof CallExpression))
            return (false);
        
        CallExpression      ce = (CallExpression) e;
        
        return (
            ce.args.length == 1 &&
            ce.name.equals (func) &&
            isThis (ce.getArgument ())
        );            
    }

}