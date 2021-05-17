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

import com.epam.deltix.util.jcg.JExpr;

import java.io.IOException;

class CSConstructorImpl extends ConstructorImpl {
    private JCompStmtElem delegate;

    CSConstructorImpl (CSClassImpl container, int modifiers) {
        super (container, modifiers, null);
    }

    @Override
    public void                     callParent(final JExpr... args) {
        delegate = new JStatementImplBase(context) {
            @Override
            public void printElement(SourceCodePrinter out) throws IOException {
                out.print(": base (");
                CSharpSrcGenContext.px(out, args);
                out.print(")");
            }
        };
    }

    @Override
    public void                     call(final JExpr... args) {
        delegate = new JStatementImplBase(context) {
            @Override
            public void printElement(SourceCodePrinter out) throws IOException {
                out.print(": this (");
                CSharpSrcGenContext.px(out, args);
                out.print(")");
            }
        };
    }

    @Override
    protected void printExceptions(SourceCodePrinter out) throws IOException {
        // cs does not allow exceptions declarations

        // also print calls of delegating constructor respecting cs notation

        if (delegate != null)
            delegate.printElement(out);
    }

    @Override
    public void printHead(SourceCodePrinter out) throws IOException {
        super.printHead(out);
    }
}
