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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import java.util.Arrays;

/**
 *
 */
public abstract class ComplexExpression extends Expression {
    public final Expression []                  args;

    protected ComplexExpression (long location, Expression ... args) {
        super (location);
        this.args = args;
    }

    public Expression           getArgument () {
        if (args.length != 1)
            throw new IllegalStateException ("#args = " + args.length);

        return (args [0]);
    }

    public final Expression     getLeft () {
        if (args.length != 2)
            throw new IllegalStateException ("#args = " + args.length);

        return (args [0]);
    }

    public final Expression     getRight () {
        if (args.length != 2)
            throw new IllegalStateException ("#args = " + args.length);

        return (args [1]);
    }

    protected final void        printBinary (
        int                         outerPriority,
        String                      infix,
        int                         thisPriority,
        InfixAssociation            thisAssociation,
        StringBuilder               s
    )
    {
        boolean                     parenthesize = outerPriority > thisPriority;

        if (parenthesize)
            s.append ("(");

        args [0].print (thisAssociation == InfixAssociation.LEFT ? thisPriority : thisPriority + 1, s);
        s.append (" ");
        s.append (infix);
        s.append (" ");
        args [1].print (thisAssociation == InfixAssociation.RIGHT ? thisPriority : thisPriority + 1, s);

        if (parenthesize)
            s.append (")");
    }

    protected final void        printPrefix (
        int                         outerPriority,
        String                      prefix,
        int                         thisPriority,
        StringBuilder               s
    )
    {
        int                         intPriority;
        
        boolean                     parenthesize = outerPriority > thisPriority;

        if (parenthesize) {
            s.append ("(");
            intPriority = OpPriority.OPEN;
        }
        else
            intPriority = thisPriority;

        s.append (prefix);
        args [0].print (intPriority, s);
        
        if (parenthesize)
            s.append (")");
    }

    protected final void        printPostfix (
        int                         outerPriority,
        String                      postfix,
        int                         thisPriority,
        StringBuilder               s
    )
    {
        int                         intPriority;

        boolean                     parenthesize = outerPriority > thisPriority;

        if (parenthesize) {
            s.append ("(");
            intPriority = OpPriority.OPEN;
        }
        else
            intPriority = thisPriority;

        args [0].print (intPriority, s);
        s.append (postfix);

        if (parenthesize)
            s.append (")");
    }

    protected final void        printCommaSepArgs (int from, int to, StringBuilder s) {
        if (to > from) {
            args [from].print (s);

            for (int ii = from + 1; ii < to; ii++) {
                s.append (", ");
                args [ii].print (s);
            }
        }
    }    
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            Arrays.equals (args, ((ComplexExpression) obj).args)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + Arrays.hashCode (args));
    }
}