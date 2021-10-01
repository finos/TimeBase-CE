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
import java.util.*;
import static com.epam.deltix.util.jcg.scg.JavaSrcGenContext.*;

/**
 *
 */
public class JCompStmtImpl 
    extends JStatementImplBase 
    implements Printable, JCompoundStatement 
{
    private final List <JCompStmtElem>      statements = new ArrayList <> ();

    public JCompStmtImpl (JContextImpl context) {
        super (context);
    }
    
    protected final void    add (JCompStmtElem e) {
        statements.add (e);
    }

    @Override
    public void             addComment (final String text) {
        statements.add (
            new JCompStmtElem () {
                @Override
                public void printElement (SourceCodePrinter out) throws IOException {
                    for (String s : text.split ("\\n")) {
                        out.newLine ();
                        out.print ("// ", s);
                    }
                }
            }
        );
    }

    @Override
    public boolean isEmpty() {
        return statements.isEmpty();
    }

    @Override
    public void printElement(SourceCodePrinter out) throws IOException {
        print(out);
    }

    @Override
    public void     print (SourceCodePrinter out) throws IOException {
        out.print ("{");
        out.indent (+1);

        for (JCompStmtElem s : statements) {
            out.newLine ();
            s.printElement (out);
        }

        out.indent (-1);
        out.newLine ();
        out.print ("}");
        out.newLine ();
    }

    @Override
    public void     add (JStatement stmt) {
        add ((JCompStmtElem) stmt);
    }

    @Override
    public void addFront(JStatement stmt) {
        statements.add (0, (JCompStmtElem)stmt);
    }

    @Override
    public void     add (JExpr e) {
        add (etos ((JExprImplBase) e));
    }

    @Override
    public JLocalVariable  addVar (int modifiers, Class <?> type, String name) {
        return (addVar (modifiers, type, name, null));
    }

    @Override
    public JLocalVariable  addVar (int modifiers, Class <?> type, String name, JExpr initValue) {
        return (addVar (modifiers, context.classToType (type), name, initValue));
    }

    @Override
    public JInitVariable addVar(int modifiers, Class<?> type, Class<?>[] typeArgs, String name, JExpr initValue) {
        return addVar(modifiers, context.classToType(type), context.classesToType(typeArgs), name, initValue);
    }

    @Override
    public JLocalVariable  addVar (int modifiers, JType type, String name) {
        return (addVar (modifiers, type, name, null));
    }

    @Override
    public JLocalVariable  addVar (int modifiers, JType type, String name, JExpr initValue) {
        VarDeclImpl vdecl = 
            new VarDeclImpl (
                context, 
                context.refineModifiersForLocalVarDecl (modifiers), 
                cn (type), 
                name
            );

        if (initValue != null)
            vdecl.setInitValue (initValue);

        statements.add (vdecl);

        return (vdecl);
    }

    @Override
    public JLocalVariable  addVar (int modifiers, JType type, JType[] typeArgs, String name, JExpr initValue) {
        VarDeclImpl vdecl =
                new VarDeclImpl(
                        context,
                        context.refineModifiersForLocalVarDecl(modifiers),
                        cn(type),
                        cn(typeArgs),
                        name
                );

        if (initValue != null)
            vdecl.setInitValue (initValue);

        statements.add (vdecl);

        return (vdecl);
    }
}