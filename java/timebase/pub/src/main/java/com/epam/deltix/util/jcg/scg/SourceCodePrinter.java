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
import java.lang.reflect.Modifier;

/**
 *  
 */
public class SourceCodePrinter {
    protected final Appendable          out;
    private int                         indent = 4;
    private int                         currentIndent = 0;
    private boolean                     atNewLine = true;
    JClass                              currentClass;
    
    public SourceCodePrinter (Appendable out) {
        this.out = out;
    }

    public int                      getIndent () {
        return indent;
    }

    public void                     setIndent (int indent) {
        this.indent = indent;
    }

    public void                     finish () throws IOException {        
    }
    
    public void                     newLine () throws IOException {
        if (!atNewLine)
            println ();
    }

    public void                     indent (int offset) throws IOException {
        currentIndent += offset * indent;
    }

    private void                    printIndentIfNewLine () throws IOException {
        if (atNewLine) {
            for (int ii = 0; ii < currentIndent; ii++)
                out.append (' ');

            atNewLine = false;
        }
    }
    
    public void                     println () throws IOException {
        out.append ('\n');
        atNewLine = true;
    }

    public void                     println (Object ... args) throws IOException {
        print (args);
        println ();    
    }

    public void                     printRefClassName (String cn) throws IOException {
        print (cn);
    }
    
    public void                     print (String s)
        throws IOException
    {
        printIndentIfNewLine ();
        
        out.append (s);
    }
    
    public void                     print (Object ... args)
        throws IOException
    {
        printIndentIfNewLine ();
        
        for (Object arg : args) {
            if (arg instanceof Printable)
                ((Printable) arg).print (this);
            else if (arg instanceof ClassImpl)
                ((ClassImpl) arg).printDeclaration (this);
            else
                print (arg.toString ());
        }
    }

    public void                     printf (String fmt, Object ... args)
        throws IOException
    {
        printIndentIfNewLine ();
        out.append (String.format (fmt, args));
    }

    public void                     printModifiers (int mods) throws IOException {
        printIndentIfNewLine ();
        
        if ((mods & Modifier.PUBLIC) != 0)
            out.append ("public ");
        else if ((mods & Modifier.PROTECTED) != 0)
            out.append ("protected ");
        else if ((mods & Modifier.PRIVATE) != 0)
            out.append ("private ");

        if ((mods & Modifier.NATIVE) != 0)
            out.append ("native ");

        if ((mods & Modifier.STATIC) != 0)
            out.append ("static ");

        if ((mods & Modifier.ABSTRACT) != 0)
            out.append ("abstract ");

        if ((mods & Modifier.FINAL) != 0)
            out.append ("final ");

        if ((mods & Modifier.TRANSIENT) != 0)
            out.append ("transient ");

        if ((mods & Modifier.VOLATILE) != 0)
            out.append ("volatile ");

        if ((mods & Modifier.SYNCHRONIZED) != 0)
            out.append ("synchronized ");

        if ((mods & Modifier.STRICT) != 0)
            out.append ("strict ");
    }
}
