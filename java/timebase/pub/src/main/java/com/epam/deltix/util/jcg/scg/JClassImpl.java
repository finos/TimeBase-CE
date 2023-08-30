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

import static com.epam.deltix.util.jcg.scg.JContextImpl.px;

/**
 *
 */
final class JClassImpl extends ClassImpl {
    JClassImpl (ClassImpl outer, int modifiers, String simpleName, String parentName) {
        super (outer, modifiers, simpleName, parentName);
    }

    JClassImpl (JContextImpl context, int modifiers, String packageName, String simpleName, String parentName) {
        super (context, modifiers, packageName, simpleName, parentName);
    }

    @Override
    public void         printDeclaration (SourceCodePrinter out)
        throws IOException
    {
        out.newLine ();

        JClass save = out.currentClass;
        out.currentClass = this;

        printAnnotations (out);
        
        out.printModifiers(modifiers());
        out.print ("class ", name ());

        if (parentName != null) {
            out.print (" extends ");
            out.printRefClassName (parentName);
        }
        
        int             n = interfaceNames.size ();

        if (n > 0) {
            out.print (" implements ", interfaceNames.get (0));

            for (int ii = 1; ii < n; ii++)
                out.print (", ", interfaceNames.get (ii));
        }

        out.print (" {");
        out.indent (1);

        for (JMemberIntf m : members)
            m.printDeclaration (out);

        out.indent (-1);
        out.newLine ();
        out.println ("}");

        out.currentClass = save;
    }

    @Override
    public JExpr callSuperMethod (final String name, final JExpr ... args) {
        return (
            new JExprImplBase (context) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print ("super.", name, " (");
                    px (out, args);
                    out.print (")");
                }
            }
        );
    }
    
    @Override
    ConstructorImpl         newConstructor (int modifiers) {
        return new JConstructorImpl (this, modifiers);
    }
    
    @Override
    ClassImpl               innerClassImpl (int modifiers, String simpleName, String parentName) {
        return new JClassImpl (this, modifiers, simpleName, parentName);
    }   

    @Override
    JMethodImpl             createMethod (int modifiers, String typeName, String name) {
        return (new JMethodImpl (this, modifiers, typeName, name));
    }        
}