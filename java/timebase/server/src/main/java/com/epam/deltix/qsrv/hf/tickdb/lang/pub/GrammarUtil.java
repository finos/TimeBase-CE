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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

import com.epam.deltix.util.io.UncheckedIOException;

import java.io.*;
import java.util.Hashtable;
import java.util.Map;

/**
 *
 */
public class GrammarUtil {
    public static String        escapeStringLiteral (CharSequence str) {
        StringBuilder       out = new StringBuilder ();

        escapeStringLiteral (str, out);

        return (out.toString ());
    }

    public static void          escapeStringLiteral (
        CharSequence                str,
        StringBuilder               out
    )
    {
        try {
            escapeStringLiteral (str, (Appendable) out);
        } catch (IOException iox) {
            throw new RuntimeException (iox);
        }
    }
    
    public static void          escapeStringLiteral (
        CharSequence                str, 
        Appendable                  out
    ) 
        throws IOException
    {
        out.append ('\'');

        int sz = str.length();

        for (int i = 0; i < sz; i++) {
            char ch = str.charAt (i);

            if (ch == '\'')
                out.append ("\\'");
            else if (ch == '\\')
                out.append("\\\\");
            else if (ch == '\t')
                out.append("\\t");
            else if (ch == '\b')
                out.append("\\b");
            else if (ch == '\r')
                out.append("\\r");
            else if (ch == '\f')
                out.append("\\f");
            else if (ch == '\n')
                out.append("\\n");
            else
                out.append (ch);
        }

        out.append ('\'');
    }

    public static String        escapeIdentifier (
        NamedObjectType             type,
        CharSequence                str
    ) 
    {
        StringBuilder       out = new StringBuilder ();

        escapeIdentifier (type, str, out);

        return (out.toString ());
    }

    private static boolean      shouldEscapeIdWithChar (
        NamedObjectType             type,
        boolean                     first,
        char                        ch
    )
    {
        if (ch >= 'A' && ch <= 'Z')
            return (false);

        if (!first && ch >= '0' && ch <= '9')
            return (false);

        switch (type) {
            case TYPE:
                if (!first && ch == '.')
                    return (false);
        }
        
        return (true);
    }

    public static void          escapeVarId (
        CharSequence                str, 
        StringBuilder               out
    ) 
    {
        escapeIdentifier (NamedObjectType.VARIABLE, str, out);
    }
    
    public static void          escapeIdentifier (
        NamedObjectType             type,
        CharSequence                str, 
        StringBuilder               out
    ) 
    {
        try {
            escapeIdentifier (type, str, (Appendable) out);
        } catch (IOException iox) {
            throw new RuntimeException (iox);
        }
    }
    
    public static void          escapeIdentifier (
        NamedObjectType             type,
        CharSequence                str, 
        Appendable                  out
    ) 
        throws IOException 
    {
        boolean     needEscape = false;

        for (int i = 0, sz = str.length(); !needEscape && i < sz; i++)
            needEscape = shouldEscapeIdWithChar (type, i == 0, str.charAt (i));

        escapeIdentifier(str, out, needEscape);
    }

    public static void          escapeEnumIdentifier (
            NamedObjectType             type,
            CharSequence                str,
            Appendable                  out
    )
        throws IOException
    {
        escapeIdentifier(str, out, true);
    }

    private static void          escapeIdentifier (
            CharSequence                str,
            Appendable                  out,
            boolean                     needEscape
    )
            throws IOException
    {
        int         sz = str.length();

        if (!needEscape)
            out.append (str);
        else {
            out.append ('\"');

            for (int i = 0; i < sz; i++) {
                char ch = str.charAt (i);

                if (ch == '\"')
                    out.append ("\"\"");
                else
                    out.append (ch);
            }

            out.append ('\"');
        }
    }
    
    private static void     printHeader (
        ClassDescriptor         cd,
        Writer                  out
    )
        throws IOException
    {
        final String    name = cd.getName ();
        final String    title = cd.getTitle ();
        
        escapeIdentifier (NamedObjectType.TYPE, name, out);
        
        if (title != null && !title.isEmpty () && !name.equals (title)) {
            out.append (' ');
            escapeStringLiteral (title, out);                        
        }
    }
    
    private static void      printComment (
        String                  sep,
        NamedDescriptor         nd,   
        Writer                  out
    ) 
        throws IOException
    {
        String                  doc = nd.getDescription ();
        
        if (doc != null && !doc.isEmpty ()) {
            out.write (sep);
            out.write ("COMMENT ");
            escapeStringLiteral (doc, out);
        }            
    }
    
    public static void      describe (
        String                  indent,
        EnumClassDescriptor     ecd,
        Writer                  out
    ) 
        throws IOException
    {
        out.append (indent);
        out.append ("ENUM ");        
        printHeader (ecd, out);                
        out.append (" (\n");
        
        String                  findent = indent + "    ";
        EnumValue []            values = ecd.getValues ();
        int                     last = values.length - 1;
        
        if (last >= 0) {
            for (int ii = 0; ; ii++) { 
                EnumValue       v = values [ii];
                
                out.write (findent);
            
                escapeEnumIdentifier(NamedObjectType.VARIABLE, v.symbol, out);
                
                out.write (" = ");
                
                out.write (String.valueOf (v.value));  
                
                if (ii == last) {
                    out.write ("\n");
                    break;
                }
                
                out.write (",\n");
            }
        }
        
        out.write (indent);
        out.write (")");
        
        String      subindent = "\n" + indent + "    ";
        
        if (ecd.isBitmask ()) {
            out.write (subindent);
            out.write ("FLAGS");
        }
        
        printComment (subindent, ecd, out);
        out.write (";\n");
    }
    
    public static String    describe (DataType type, boolean baseOnly) {
        StringWriter    swr = new StringWriter ();
        
        try {
            describe (type, baseOnly, swr);
        } catch (IOException iox) {
            throw new RuntimeException ();
        }
        
        return (swr.toString ());
    }
    
    //#######################################################################
    //  IF YOU ARE CHANGING THIS METHOD, YOU PROBABLY NEED TO CHANGE THE
    //  TYPE EDITING GUI IN CLASS DynamicFieldEditor!!!
    //#######################################################################
    public static void      describe (
        DataType                type,
        boolean                 baseOnly,
        Writer                  out
    ) 
        throws IOException
    {
        escapeIdentifier(NamedObjectType.TYPE, type.getBaseName(), out);

        if (type instanceof ArrayDataType)
            describeArrayContentType((ArrayDataType) type, out);
        else if (type instanceof ClassDataType)
            describeClassContentType((ClassDataType) type, out);
        
        if (!type.isNullable ())
            out.append (" NOT NULL");
        
        if (type instanceof IntegerDataType) {
            IntegerDataType     it = (IntegerDataType) type;
            
            switch (it.getSize ()) {
                case 1: out.append (" " + DataTypeCompiler.SIGNED + " (8)"); break;
                case 2: out.append (" " + DataTypeCompiler.SIGNED + " (16)"); break;
                case 4: out.append (" " + DataTypeCompiler.SIGNED + " (32)"); break;
                case 6: out.append (" " + DataTypeCompiler.SIGNED + " (48)"); break;
                    
                case 8: /* default */ break;
                    
                case IntegerDataType.PACKED_UNSIGNED_INT:
                    out.append (" " + DataTypeCompiler.UNSIGNED + " (30)");
                    break;
                
                case IntegerDataType.PACKED_UNSIGNED_LONG:
                    out.append (" " + DataTypeCompiler.UNSIGNED + " (61)");
                    break;
                    
                case IntegerDataType.PACKED_INTERVAL:
                    out.append (" " + DataTypeCompiler.INTERVAL);
                    break;
                    
                default:
                    out.append (it.getEncoding ()).append("??");
                    break;
            }

            if (it.min != null || it.max != null) {
                out.append (" BETWEEN ");
                if (it.min == null)
                    out.append ('*');
                else
                    out.append (it.min.toString ());
                out.append (" AND ");
                if (it.max == null)
                    out.append ('*');
                else
                    out.append (it.max.toString ());
            }            
        }        
        else if (type instanceof FloatDataType) {
            FloatDataType       ft = (FloatDataType) type;
            
            switch (ft.getScale ()) {
                case FloatDataType.FIXED_FLOAT:
                    out.append (' ');
                    out.append (DataTypeCompiler.BINARY);
                    out.append (" (32)");
                    break;
                
                case FloatDataType.FIXED_DOUBLE:
                    // Is the default
                    break;
                
                case FloatDataType.SCALE_AUTO:
                    out.append (' ');
                    out.append (DataTypeCompiler.DECIMAL);
                    break;

                case FloatDataType.SCALE_DECIMAL64:
                    out.append (' ');
                    out.append (DataTypeCompiler.DECIMAL64);
                    break;

                default:
                    out.append (' ');
                    out.append (DataTypeCompiler.DECIMAL);
                    out.append (" (");
                    out.append (String.valueOf(ft.getScale ()));
                    out.append (')');
                    break;
            }
            
            if (ft.min != null || ft.max != null) {
                out.append (" BETWEEN ");
                if (ft.min == null)
                    out.append ('*');
                else
                    out.append (ft.min.toString ());
                out.append (" AND ");
                if (ft.max == null)
                    out.append ('*');
                else
                    out.append (ft.max.toString ());
            }    
        }
        else if (type instanceof VarcharDataType) {
            VarcharDataType     vt = (VarcharDataType) type;
        
            switch (vt.getEncodingType ()) {
                case VarcharDataType.INLINE_VARSIZE:
                    if (vt.isMultiLine ()) {
                        out.append(" ");
                        out.append(DataTypeCompiler.MULTILINE);
                    }
                    break;
                    
                case VarcharDataType.ALPHANUMERIC:
                    out.append (' ');
                    out.append (DataTypeCompiler.ALPHANUMERIC);
                    out.append (" (");
                    out.append (String.valueOf(vt.getLength ()));
                    out.append (')');
                    break;
                    
                default:
                    out.append (vt.getEncoding ());
                    out.append (" ??");
                    break;
            }
        }
    }

    private static void    describeArrayContentType(ArrayDataType arrayDataType, Writer out) throws IOException {
        out.append('(');

        DataType elementType = arrayDataType.getElementDataType();
        describe(elementType, false, out);

        out.append(')');
    }

    private static void    describeClassContentType(ClassDataType classDataType, Writer out) throws IOException {
        out.append('(');

        RecordClassDescriptor[] descriptors = classDataType.getDescriptors();
        boolean first = true;
        for (RecordClassDescriptor descriptor : descriptors) {
            if (first)
                first = false;
            else
                out.append(", ");

            escapeIdentifier(NamedObjectType.TYPE, descriptor.getName(), out);
        }

        out.append(')');
    }
    
    public static void     describe (
        DataField               df,
        Writer                  out        
    ) 
        throws IOException
    {
        String                  name = df.getName ();
        String                  title = df.getTitle ();
        DataType                type = df.getType ();
        boolean                 isStatic = (df instanceof StaticDataField);
        
        if (isStatic)
            out.write ("STATIC ");
                        
        escapeIdentifier (NamedObjectType.VARIABLE, name, out);
        out.write (' ');
        
        if (title != null && !title.equals (name)) {            
            escapeStringLiteral (title, out);
            out.write (' ');
        }

        describe(type, isStatic, out);
        
        if (isStatic) {
            out.append (" = ");
            
            StaticDataField     sdf = (StaticDataField) df;
            String              value = sdf.getStaticValue ();
            
            if (value == null)
                out.write ("NULL");
            else if (type instanceof IntegerDataType ||
                type instanceof FloatDataType ||
                type instanceof BooleanDataType)
                out.write (value);
            else if (type instanceof EnumDataType) 
                escapeIdentifier (
                    NamedObjectType.VARIABLE,
                    value,
                    out
                );
            else if (type instanceof VarcharDataType)
                escapeStringLiteral (value, out);
            else if (type instanceof TimeOfDayDataType) {
                escapeStringLiteral (value, out);
                out.append ('T');
            }
            else if (type instanceof DateTimeDataType) {
                escapeStringLiteral (value, out);
                out.append ('D');
            }
            else if (type instanceof CharDataType) {
                escapeStringLiteral (value, out);
                out.append ('C');
            }
            else if (type instanceof BinaryDataType) {
                escapeStringLiteral (value, out);
                out.append ('X');
            }
            else {
                out.append ("?? ");
                out.append (value);
            }
        }
        else {
            NonStaticDataField  nsdf = (NonStaticDataField) df;
            String              r2 = nsdf.getRelativeTo ();
            
            if (r2 != null) {
                out.write (" RELATIVE TO ");
                escapeIdentifier (NamedObjectType.VARIABLE, r2, out);
            }
        }

        describe(out, df.getAttributes());

        printComment (" ", df, out);
    }

    private static void describe(Writer out, Map<String, String> allTags) throws IOException {
        if (allTags != null) {
            // todo: temporary hack (displayIdentifier is always presents in the map)
            // skip displayIdentifier if it false
            Hashtable<String, String> tags = new Hashtable<>(allTags);
            String displayIdentifier = tags.get("displayIdentifier");
            if ("false".equalsIgnoreCase(displayIdentifier)) {
                tags.remove("displayIdentifier");
            }

            if (tags.size() > 0) {
                out.append(" TAGS (");
                boolean first = true;
                for (Map.Entry<String, String> tag : tags.entrySet()) {
                    if (first) {
                        first = false;
                    } else {
                        out.append(",");
                    }

                    escapeIdentifier(NamedObjectType.VARIABLE, tag.getKey(), out);
                    out.append(":");
                    escapeIdentifier(NamedObjectType.VARIABLE, tag.getValue(), out);
                }
                out.append(")");
            }
        }
    }
    
    public static void      describe (
        String                  indent,
        RecordClassDescriptor   rcd,   
        boolean                 auxiliary,
        Writer                  out
    ) 
        throws IOException
    {
        RecordClassDescriptor   parent = rcd.getParent ();
        
        out.write (indent);
        out.write ("CLASS ");
        printHeader (rcd, out);
        
        if (parent != null) {
            out.write (" UNDER ");
            escapeIdentifier (NamedObjectType.TYPE, parent.getName (), out);
        }
        
        out.write (" (\n");
        
        String                  findent = indent + "    ";
        DataField []            fields = rcd.getFields ();
        int                     last = fields.length - 1;
        
        if (last >= 0) {
            for (int ii = 0; ; ii++) { 
                out.write (findent);
            
                describe (fields [ii], out);

                if (ii == last) {
                    out.write ('\n');
                    break;
                }
                
                out.write (",\n");
            }
        }
        
        out.write (indent);
        out.write (")");
        
        String      subindent = "\n" + indent + "    ";
                
        if (auxiliary) {
            out.write (subindent);
            out.write ("AUXILIARY");
        }

        if (rcd.isAbstract ()) {
            out.write (subindent);
            out.write ("NOT INSTANTIABLE");
        }
        
        printComment (subindent, rcd, out);
        out.write (";\n");
    }
    
    public static String    describe (
        String                  indent,
        ClassDescriptor         cd
    )
    {
        StringWriter    swr = new StringWriter ();
        
        try {
            describe (indent, cd, false, swr);
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
        
        return (swr.toString ());
    }
    
    public static void      describe (
        String                  indent,
        ClassDescriptor         cd,   
        boolean                 auxiliary,
        Writer                  out
    ) 
        throws IOException
    {        
        if (cd instanceof EnumClassDescriptor)
            describe (indent, (EnumClassDescriptor) cd, out);
        else if (cd instanceof RecordClassDescriptor)
            describe (indent, (RecordClassDescriptor) cd, auxiliary, out);
        else
            throw new RuntimeException (cd.toString ());        
    }    
    
    @SuppressWarnings ("unchecked")
    public static void      writeOptions (
        OptionProcessor []      ops,
        Object                  source,
        Writer                  out
    )
        throws IOException
    {
        
        if (ops != null) {
            StringBuilder           sb = new StringBuilder ();
                        
            for (OptionProcessor op : ops) {                
                int             pos = sb.length ();
                boolean         ok = op.print (source, sb);
                
                if (pos != 0 && ok)
                    sb.insert (pos, "; ");                
            }

            if (sb.length () != 0) {
                out.write ("OPTIONS (");
                out.write (sb.toString ());
                out.write (")\n");
            }
        }
    }
    
    public static void      describe (
        String                  indent,
        DXTickStream            s,   
        Writer                  out
    )
        throws IOException
    {
        StreamOptions           sops = s.getStreamOptions ();
        String                  key = s.getKey ();
        
        out.write (indent);
        out.write (s.getScope ().name ());        
        out.write (" STREAM ");
        escapeIdentifier (NamedObjectType.VARIABLE, key, out);
        
        if (sops.name != null && !sops.name.equals (key)) {
            out.write (' ');
            escapeStringLiteral (sops.name, out);
        }
        
        out.write (" (\n"); 
        
        String                  subindent = indent + "    ";
        
        RecordClassSet recordClassSet = s.getStreamOptions().getMetaData();
        for (ClassDescriptor cd : ClassDescriptor.depSort (s.getAllDescriptors ())) {
            boolean auxiliary = recordClassSet.getContentClass(cd.getGuid()) == null;
            GrammarUtil.describe(subindent, cd, auxiliary, out);
        }

        out.write (indent);
        out.write (")\n");
        out.write (indent);
        
        writeOptions (StreamOptionsProcessors.forStream (s), sops, out);
        
        if (sops.description != null && sops.description.length () != 0) {
            out.write ("COMMENT ");
            escapeStringLiteral (sops.description, out);
            out.write ("\n");
        }            
    }
}