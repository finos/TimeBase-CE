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
package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;

import java.io.IOException;
import java.util.ArrayList;

public class JArrayInitializerImpl extends JExprImplBase implements JArrayInitializer {
    //private final Class<?> javaType;
    private final ArrayList<JExpr> values = new ArrayList <> ();

    @Override
    public void print(int outerPriority, SourceCodePrinter out) throws IOException {
        // "new Type[]" must be added to initialize a local variable 
        out.print("{ ");
        final int size = values.size();
        for (int i = 0; i < size; i++) {
            out.print(values.get(i));
            if(i<size -1)
                out.print(", ");

        }
        out.print(" }");
    }

    public JArrayInitializerImpl (JContextImpl context) {
        super (context);
    }

    @Override
    public void add(JExpr value) {
        values.add(value);
    }
}