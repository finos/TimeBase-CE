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
import java.lang.reflect.Modifier;
import java.util.*;
import static com.epam.deltix.util.jcg.scg.JavaSrcGenContext.*;

/**
 *
 */
abstract class CallableImpl extends JMemberImpl implements JCallable {
    private final List <VariableImpl>  args = new ArrayList <> ();
    private final List <String>        exceptions = new ArrayList <> ();
    protected final JCompoundStatement body;

    public CallableImpl (
        ClassImpl                   container, 
        int                         modifiers, 
        String                      name
    )
    {
        super (modifiers, name, container);
        
        body = new JCompStmtImpl (context);
    }

    @Override
    public JCompoundStatement       body () {
        return (body);
    }

    @Override
    public JMethodArgument          addArg (
        int                             modifiers,
        Class <?>                       type,
        String                          name
    )
    {
        return (addArg (modifiers, context.classToType (type), name));
    }

    @Override
    public JMethodArgument          addArg (
        int                             modifiers,
        JType                           type,
        String                          name
    )
    {
        VariableImpl   v = 
            new VariableImpl (
                context, 
                context.refineModifiersForMethodArg (modifiers), 
                cn (type), 
                name
            );

        args.add (v);

        return (v);
    }

    abstract void                   printHead (SourceCodePrinter out)
        throws IOException;

    void                            printModifiers (SourceCodePrinter out) 
        throws IOException
    {
        context.printModifiers (modifiers(), out);
    }
    
    @Override
    public void                     printDeclaration (SourceCodePrinter out)
        throws IOException
    {
        out.newLine ();

        printModifiers (out);
        
        printHead (out);

        out.print (" (");

        int         n = args.size ();

        if (n == 1) 
            args.get (0).printHead (out);        
        else if (n > 1) {
            out.indent (1);
            out.newLine ();
            args.get (0).printHead (out);

            for (int ii = 1; ii < n; ii++) {
                out.println (",");
                args.get (ii).printHead (out);
            }

            out.indent (-1);
            out.newLine ();
        }

        out.println (")");

        printExceptions(out);

        out.print (body);
    }

    protected void printExceptions(SourceCodePrinter out)
            throws IOException
    {
        int n = exceptions.size ();

        if (n > 0) {
            out.indent (1);
            out.print ("throws ", exceptions.get (0));

            for (int ii = 1; ii < n; ii++)
                out.print (", ", exceptions.get (ii));

            out.indent (-1);
        }
    }

    public void addException(Class<? extends Throwable> throwable) {
        //TODO - improve
        if (context instanceof JavaSrcGenContext)
            exceptions.add (throwable.getName ());
    }
}