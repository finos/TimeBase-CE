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

import com.epam.deltix.util.jcg.JClass;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JInitMemberVariable;

import java.io.IOException;

public class ThisVariableImpl extends InitVariableImpl
    implements JInitMemberVariable, JMemberIntf
{
    private JClass containerClass;

    ThisVariableImpl (ClassImpl containerClass) {
        super (containerClass.context, 0, null, null);
        this.containerClass = containerClass;
    }

    @Override
    public JClass containerClass() {
        return containerClass;
    }

    @Override
    public JExpr access () {
        return (
            new JExprImplBase (context) {
                @Override
                public void print (int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print ("this");
                }
            }
        );
    }

    @Override
    public JExpr            access (final JExpr obj) {
        if (obj == null)
            throw new IllegalArgumentException ("null object");

        return (
            new JExprImplBase (context) {
                @Override
                public void print (int outerProprity, SourceCodePrinter out) throws IOException {
                    printPostfix (
                        outerProprity, 
                        (JExprImplBase) obj, 
                        ".this", JavaOpPriority.POSTFIX, 
                        out
                    );
                }
            }
        );
    }

    @Override
    public void             print (int outerProprity, SourceCodePrinter out)
        throws IOException
    {
        throw new RuntimeException ("should not be called");
    }

    @Override
    public void addComment(String comment) {

    }
}