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
import static com.epam.deltix.util.jcg.scg.JavaSrcGenContext.*;

/**
 *
 */
abstract class JExprImplBase implements JExpr, Printable {
    final JContextImpl          context;
    
    public JExprImplBase (JContextImpl context) {
        this.context = context;
    }
    
    @Override
    public final void           print (SourceCodePrinter out) 
        throws IOException 
    {
        print (JavaOpPriority.OPEN, out);
    }
    
    abstract void               print (int outerPriority, SourceCodePrinter out) 
        throws IOException;
        
    protected final void        printBinary (
        int                         outerPriority,
        JExprImplBase               left,
        String                      infix,
        int                         thisPriority,
        InfixAssociation            thisAssociation,
        JExprImplBase               right,
        SourceCodePrinter           out
    )
        throws IOException
    {
        boolean                     parenthesize = outerPriority > thisPriority;

        if (parenthesize)
            out.print ("(");

        left.print (thisAssociation == InfixAssociation.LEFT ? thisPriority : thisPriority + 1, out);
        out.print (infix);
        right.print (thisAssociation == InfixAssociation.RIGHT ? thisPriority : thisPriority + 1, out);

        if (parenthesize)
            out.print (")");
    }

    protected final void        printPrefix (
        int                         outerPriority,
        String                      prefix,
        int                         thisPriority,
        JExprImplBase               right,
        SourceCodePrinter           out
    )
        throws IOException
    {
        int                         intPriority;
        
        boolean                     parenthesize = outerPriority > thisPriority;

        if (parenthesize) {
            out.print ("(");
            intPriority = JavaOpPriority.OPEN;
        }
        else
            intPriority = thisPriority;

        out.print (prefix);
        right.print (intPriority, out);
        
        if (parenthesize)
            out.print (")");
    }

    protected final void        printPostfix (
        int                         outerPriority,
        JExprImplBase               left,
        String                      postfix,
        int                         thisPriority,
        SourceCodePrinter           out
    )
        throws IOException
    {
        int                         intPriority;
        boolean                     parenthesize = outerPriority > thisPriority;

        if (parenthesize) {
            out.print ("(");
            intPriority = JavaOpPriority.OPEN;
        }
        else
            intPriority = thisPriority;

        left.print (intPriority, out);
        out.print (postfix);

        if (parenthesize)
            out.print (")");
    }
    
    abstract static class X2 extends JExprImplBase {
        protected final JExpr   arg;

        public X2 (JExprImplBase arg) {
            super (arg.context);
            
            this.arg = arg;
        }
    }

    @Override
    public JExpr cast  (final Class<?> toClass) {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) 
                    throws IOException 
                {
                    boolean         parenthesize = outerPriority > JavaOpPriority.CAST;
                    int             intPriority;
                    
                    if (parenthesize) {
                        out.print ("(");
                        intPriority = JavaOpPriority.OPEN;
                    }
                    else
                        intPriority = JavaOpPriority.CAST;

                    out.print ("(");
                    out.printRefClassName (context.cn1 (toClass));
                    out.print (")");

                    ((JExprImplBase) arg).print (intPriority, out);
                    
                    if (parenthesize)
                        out.print (")");                    
                }
            }
        );
    }

    @Override
    public JExpr        incAndGet () {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    printPrefix (
                        outerPriority, 
                        "++", 
                        JavaOpPriority.UNARY, 
                        (JExprImplBase) arg, 
                        out
                    );
                }
            }
        );
    }

    @Override
    public JExpr        decAndGet () {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    printPrefix (
                        outerPriority, 
                        "--", 
                        JavaOpPriority.UNARY, 
                        (JExprImplBase) arg, 
                        out
                    );
                }
            }
        );
    }

    @Override
    public JExpr        getAndInc () {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    printPostfix (
                        outerPriority, 
                        (JExprImplBase) arg, 
                        "++", 
                        JavaOpPriority.POSTFIX, 
                        out
                    );
                }
            }
        );
    }

    @Override
    public JExpr        getAndDec () {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    printPostfix (
                        outerPriority, 
                        (JExprImplBase) arg, 
                        "--", 
                        JavaOpPriority.POSTFIX, 
                        out
                    );
                }
            }
        );
    }

    @Override
    public JStatement   inc () {
        return (
            new JStatementImplBase (context) {
                @Override
                public void     printElement (SourceCodePrinter out) throws IOException {
                    out.print (JExprImplBase.this, "++;");
                }
            }
        );
    }

    @Override
    public JStatement       assign (final JExpr value) {
        return (
            new JStatementImplBase (context) {
                @Override
                public void     printElement (SourceCodePrinter out) throws IOException {
                    out.print (JExprImplBase.this, " = ", value, ";");
                }
            }
        );
    }

    @Override
    public JExpr            assignExpr (final JExpr value) {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    printBinary (
                        outerPriority, 
                        JExprImplBase.this, 
                        " = ", 
                        JavaOpPriority.ASSIGNMENT,
                        InfixAssociation.RIGHT, 
                        (JExprImplBase) value, 
                        out
                    );
                }
            }
        );
    }

    @Override
    public JStatement   dec () {
        return (
            new JStatementImplBase (context) {
                @Override
                public void     printElement (SourceCodePrinter out) throws IOException {
                    out.print (JExprImplBase.this, "--;");
                }
            }
        );
    }

    @Override
    public JStatement   alter (final String op, final JExpr arg)  {
        return (
            new JStatementImplBase (context) {
                @Override
                public void     printElement (SourceCodePrinter out) throws IOException {
                    out.print (JExprImplBase.this, " ", op, " ", arg, ";");
                }
            }
        );
    }

    @Override
    public JExpr        cast (final JType toClass) {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    boolean         parenthesize = outerPriority > JavaOpPriority.CAST;
                    int             intPriority;
                    
                    if (parenthesize) {
                        out.print ("(");
                        intPriority = JavaOpPriority.OPEN;
                    }
                    else
                        intPriority = JavaOpPriority.CAST;

                    out.print ("(");
                    out.printRefClassName (cn (toClass));
                    out.print (")");

                    ((JExprImplBase) arg).print (intPriority, out);
                    
                    if (parenthesize)
                        out.print (")");                    
                }
            }
        );
    }

    @Override
    public JExpr        not () {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    printPrefix (outerPriority, "!", JavaOpPriority.UNARY, (JExprImplBase) arg, out);
                }
            }
        );
    }

    @Override
    public JExpr negate() {
        return new X2 (this) {
            @Override
            public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                printPrefix(outerPriority, "-", JavaOpPriority.UNARY, (JExprImplBase) arg, out);
            }
        };
    }

    @Override
    public JStatement   throwStmt () {
        return (
            new JStatementImplBase (context) {
                @Override
                public void     printElement (SourceCodePrinter out) throws IOException {
                    out.print ("throw ", JExprImplBase.this, ";");
                }
            }
        );
    }

    @Override
    public JStatement   returnStmt () {
        return (
            new JStatementImplBase (context) {
                @Override
                public void     printElement (SourceCodePrinter out) throws IOException {
                    out.print ("return ", JExprImplBase.this, ";");
                }
            }
        );
    }

    @Override
    public JStatement   asStmt () {
        return (etos (this));
    }

    @Override
    public JExpr        call (final String method, final JExpr ... args) {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    int                         intPriority;
                    boolean                     parenthesize = outerPriority > JavaOpPriority.POSTFIX;

                    if (parenthesize) {
                        out.print ("(");
                        intPriority = JavaOpPriority.OPEN;
                    }
                    else
                        intPriority = JavaOpPriority.POSTFIX;

                    ((JExprImplBase) arg).print (intPriority, out);
                                                            
                    out.print (".", method, " (");
                    px (out, args);
                    out.print (")");
                    
                    if (parenthesize)
                        out.print (")");
                }
            }
        );
    }

    @Override
    public JExpr        index (final JExpr index) {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    int                         intPriority;
                    boolean                     parenthesize = outerPriority > JavaOpPriority.POSTFIX;

                    if (parenthesize) {
                        out.print ("(");
                        intPriority = JavaOpPriority.OPEN;
                    }
                    else
                        intPriority = JavaOpPriority.POSTFIX;

                    ((JExprImplBase) arg).print (intPriority, out);
                    out.print ("[", index, "]");
                    
                    if (parenthesize)
                        out.print (")");                   
                }
            }
        );
    }

    @Override
    public JExpr        index (final int index) {
        return (index (context.mkint (index)));
    }
    
    @Override
    public JExpr        field (final String fieldId) {
        return (
            new X2 (this) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    printPostfix (
                        outerPriority, 
                        (JExprImplBase) arg, 
                        "." + fieldId, 
                        JavaOpPriority.POSTFIX, 
                        out
                    );                     
                }
            }
        );
    }

    @Override
    public JSwitchStatement switchStmt () {
        return (switchStmt (null));
    }

    @Override
    public JSwitchStatement switchStmt (String label) {
        return (new JSwitchStatementImpl (label, this));
    }
}