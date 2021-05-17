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
import java.lang.reflect.Modifier;

/**
 *
 */
final class MemberVariableImpl
    extends InitVariableImpl
    implements JInitMemberVariable, JMemberIntf
{
    private final JClass    containerClass;

    MemberVariableImpl (JContextImpl context, int modifiers, String type, String name) {
        super (context, modifiers, type, name);
        this.containerClass = null;
    }

    MemberVariableImpl (ClassImpl containerClass, int modifiers, String type, String name) {
        super (containerClass.context, modifiers, type, name);
        this.containerClass = containerClass;
    }

    @Override
    public JClass           containerClass () {
        return (containerClass);
    }

    @Override
    public JExpr            access () {
        return (
            new JExprImplBase (context) {
                @Override
                public void print (int outerPriority, SourceCodePrinter out) throws IOException {
                    if (containerClass == null)
                        out.print (name ());
                    else if (Modifier.isStatic (modifiers ())) {
                        if (containerClass != out.currentClass)
                            out.print (containerClass.fullName (), ".");
                        out.print (name ());
                    }
                    else {
                        if (containerClass != out.currentClass)
                            out.print (containerClass.name (), ".");
                        
                        out.print ("this.", name ());
                    }
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
                public void print (int outerPriority, SourceCodePrinter out) throws IOException {
                    printPostfix (
                        outerPriority, 
                        (JExprImplBase) obj, 
                        "." + name (), 
                        JavaOpPriority.POSTFIX, 
                        out
                    );
                }
            }
        );
    }
    
    @Override
    public void             print (int outerPriority, SourceCodePrinter out)
        throws IOException
    {
        throw new RuntimeException ("should not be called");        
    }

    @Override
    public void addComment(String comment) {
        this.comment = comment;
    }
}
