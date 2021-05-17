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

import com.epam.deltix.util.jcg.JAnnotation;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JMemberVariable;

import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * implements .NET auto-property
 */
public class CSPropertyImpl extends JMemberImpl implements JMemberVariable {
    private final String type;

    CSPropertyImpl(int modifiers, String name, CSClassImpl container, String type) {
        super (modifiers, name, container);
        this.type = type;
    }

    @Override
    public void addAnnotation(JAnnotation annotation) {
        throw new UnsupportedOperationException("unsupported for now");
    }

    @Override
    public void printDeclaration(SourceCodePrinter out) throws IOException {
        out.newLine ();

        context.printModifiers(modifiers(), out);

        context.printType(type, out);
        
        out.print(" ", name(), " ", "{ get; set; }");
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public JExpr            access () {
        return (
                new JExprImplBase (context) {
                    @Override
                    public void print (int outerPriority, SourceCodePrinter out) throws IOException {
                        if (containerClass() == null)
                            out.print (name ());
                        else if (Modifier.isStatic(modifiers())) {
                            if (containerClass() != out.currentClass)
                                out.print (containerClass().fullName(), ".");
                            out.print (name ());
                        }
                        else {
                            if (containerClass() != out.currentClass)
                                out.print (containerClass().name(), ".");

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
                    out.print ("(", obj, ").", name ());
                }
            }
        );
    }
}
