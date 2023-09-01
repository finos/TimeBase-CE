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
package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JInitVariable;
import java.io.IOException;

/**
 *
 */
class InitVariableImpl
    extends VariableImpl
    implements JInitVariable
{
    private JExpr       initValue = null;

    InitVariableImpl(JContextImpl context, int modifiers, String type, String name) {
        super (context, modifiers, type, name);
    }

    InitVariableImpl(JContextImpl context, int modifiers, String type, String[] typeArgs, String name) {
        super (context, modifiers, type, typeArgs, name, null);
    }

    public void         printDeclaration (SourceCodePrinter out)
        throws IOException
    {
        out.newLine ();
        printHead (out);

        if (initValue != null)
            out.print (" = ", initValue);

        out.println (";");
    }

    @Override
    public void         setInitValue (JExpr value) {
        initValue = value;
    }
}