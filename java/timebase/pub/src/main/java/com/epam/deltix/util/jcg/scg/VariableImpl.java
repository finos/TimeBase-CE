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

import com.epam.deltix.util.jcg.*;

import java.io.IOException;
import java.util.*;

class VariableImpl 
    extends JExprImplBase
    implements JVariable, JMethodArgument 
{
    private final String                    name;
    private final String                    type;
    private final String[]                  typeArgs;
    private final int                       modifiers;
    protected String                        comment;
    private final Collection <JAnnotation>  annotations = new ArrayList <> ();

    VariableImpl (JContextImpl context, int modifiers, String type, String name) {
        this(context, modifiers, type, name, null);
    }

    VariableImpl (JContextImpl context, int modifiers, String type, String name, String comment) {
        this(context, modifiers, type, null, name, comment);
    }

    VariableImpl (JContextImpl context, int modifiers, String type, String[] typeArgs, String name, String comment) {
        super (context);
        
        this.name = name;
        this.type = type;
        this.typeArgs = typeArgs;
        this.modifiers = modifiers;
        this.comment = comment;
    }

    @Override
    public void             addAnnotation (JAnnotation annotation) {
        annotations.add (annotation);
    }

    @Override
    public final int modifiers() {
        return (modifiers);
    }

    @Override
    public final String name() {
        return (name);
    }

    @Override
    public String type() {
        return (type);
    }

    protected final void printHead(SourceCodePrinter out) throws IOException {

        if (comment != null) {
            out.println("/**");
            String[] lines = comment.split("\n");
            for (String line : lines)
                out.println("* " + line.replace("\r", ""));
            out.println("*/");
        }

        if (annotations.size () > 0) {
            for (JAnnotation annotation : annotations) {
                out.print (annotation);
                out.println ();
            }
        }

        context.printModifiers (modifiers, out);
        context.printType (type, typeArgs, out);

        out.print(" ", name);
    }

    /**
     * Override to reference less-than-local variables.
     */
    public void print (int outerPriority, SourceCodePrinter out)
        throws IOException 
    {
        out.print(name);
    }
}