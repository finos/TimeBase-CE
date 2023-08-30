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
import com.epam.deltix.util.jcg.JSwitchStatement;
import java.io.IOException;

/**
 *
 */
class JSwitchStatementImpl
    extends JCompStmtImpl
    implements JSwitchStatement
{
    private final String    label;
    private final JExpr     discriminator;

    JSwitchStatementImpl (String label, JExprImplBase discriminator) {
        super (discriminator.context);
        
        this.label = label;
        this.discriminator = discriminator;
    }

    @Override
    public void     print (SourceCodePrinter out) throws IOException {
        out.print (label, ": switch (", discriminator, ") ");
        super.print (out);
    }

    @Override
    public void             addBreak () {
        add (context.mkbreak (label));
    }

    @Override
    public void             addCaseLabel (final JExpr e) {
        addCaseLabel (e, null);
    }

    @Override
    public void             addCaseLabel (final JExpr e, final String comment) {
        add (
            new JCompStmtElem () {
                @Override
                public void printElement (SourceCodePrinter out) throws IOException {
                    out.indent (-1);
                    out.print ("case ", e, ":");
                    if (comment != null)
                        out.print (" // " + comment);                    
                    out.indent (1);
                }
            }
        );
    }

    @Override
    public void             addDefaultLabel () {
        add (
            new JCompStmtElem () {
                @Override
                public void printElement (SourceCodePrinter out) throws IOException {
                    out.indent (-1);
                    out.print ("default:");
                    out.indent (1);
                }
            }
        );
    }

}