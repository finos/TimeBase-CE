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

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.jcg.*;

import java.io.IOException;
import java.lang.reflect.Modifier;

final class CSClassImpl extends ClassImpl {

    CSClassImpl (ClassImpl outer, int modifiers, String simpleName, String parentName) {
        super (outer, modifiers, simpleName, parentName);
    }

    CSClassImpl (CSharpSrcGenContext context, int modifiers, String packageName, String simpleName, String parentName) {
        super (context, modifiers, packageName, simpleName, parentName);
    }

    @Override
    public JInitMemberVariable  addVar (int modifiers, JType type, String name, JExpr initValue, boolean nullable) {
        MemberVariableImpl vdecl =
            new MemberVariableImpl (
                this, modifiers, context.translateType(context.cn1(type, nullable)), name);

        if (initValue != null)
            vdecl.setInitValue(initValue);

        members.add(vdecl);

        return (vdecl);
    }

    @Override
    public JMemberVariable addProperty(int modifiers, Class<?> type, String name) {
        final JMemberVariable prop = 
            new CSPropertyImpl (modifiers, name, this, context.translateType (context.cn1(type)));
            
        members.add((JMemberIntf) prop);
        return prop;
    }

    @Override
    public void         printDeclaration (SourceCodePrinter out)
        throws IOException
    {
        out.newLine ();

        JClass save = out.currentClass;
        out.currentClass = this;

        printAnnotations (out);
        
        printClassModifiers(out);
        out.print ("class ", name ());

        if (parentName != null)
            out.print (" : ", parentName.equals(InstrumentMessage.class.getName()) ? "EPAM.Deltix.Timebase.Api.Messages.InstrumentMessage" : parentName);

        int             n = interfaceNames.size ();

        if (n > 0) {
            if (parentName == null)
                out.print(" : ", interfaceNames.get(0));
            else
                out.print(", ", interfaceNames.get(0));

            for (int ii = 1; ii < n; ii++)
                out.print (", ", interfaceNames.get (ii));
        }

        out.print (" {");
        out.indent (1);

        for (JMemberIntf m : members)
            m.printDeclaration (out);

        out.indent (-1);
        out.println ("}");

        out.currentClass = save;
    }

    private void printClassModifiers(SourceCodePrinter out) throws IOException {
        final int mods = modifiers();
        if ((mods & Modifier.PUBLIC) != 0)
            out.print ("public ");
        else if ((mods & Modifier.PROTECTED) != 0)
            out.print ("protected ");
        else if ((mods & Modifier.PRIVATE) != 0)
            out.print ("private ");
        else
            out.print ("inner ");

        if ((mods & Modifier.NATIVE) != 0)
            out.print ("native ");

        if ((mods & Modifier.STATIC) != 0)
            out.print ("static ");

        if ((mods & Modifier.ABSTRACT) != 0)
            out.print ("abstract ");

        if ((mods & Modifier.FINAL) != 0)
            out.print ("sealed ");
    }

    @Override
    public JExpr callSuperMethod(final String name, final JExpr... args) {
        return (
            new JExprImplBase (context) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print ("base.", name, " (");
                    JContextImpl.px (out, args);
                    out.print (")");
                }
            }
        );
    }
    
    @Override
    ConstructorImpl         newConstructor (int modifiers) {
        return new CSConstructorImpl (this, modifiers);
    }
    
    @Override
    ClassImpl               innerClassImpl (int modifiers, String simpleName, String parentName) {
        return new CSClassImpl (this, modifiers, simpleName, parentName);
    }
    
    @Override
    CSMethodImpl            createMethod (int modifiers, String typeName, String name) {
        return (new CSMethodImpl (this, modifiers, typeName, name));
    }
}
