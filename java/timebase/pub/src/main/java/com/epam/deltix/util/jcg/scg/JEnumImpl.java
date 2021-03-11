package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;
import com.epam.deltix.util.jcg.*;

import java.io.IOException;

public class JEnumImpl extends ClassImpl {

    private EnumClassDescriptor ecd;

    JEnumImpl (JContextImpl context, int modifiers, String packageName, String simpleName, String parentName) {
        super(context, modifiers, packageName, simpleName, parentName);
    }

    public JEnumImpl (JContextImpl context, int modifiers, String packageName, String simpleName, String parentName, EnumClassDescriptor ecd) {
        this (context, modifiers, packageName, simpleName, parentName);
        this.ecd = ecd;
    }

    @Override
    ClassImpl       innerClassImpl (int modifiers, String simpleName, String parentName) {
        throw new UnsupportedOperationException (); 
    }

    @Override
    ConstructorImpl newConstructor (int modifiers) {
        throw new UnsupportedOperationException (); 
    }

    @Override
    MethodImpl      createMethod (int modifiers, String typeName, String name) {
        throw new UnsupportedOperationException ();
    }    
    
    @Override
    public JExpr callSuperMethod(String name, JExpr... args) {
        return null;
    }

    @Override
    public void printDeclaration(SourceCodePrinter out) throws IOException {
        out.newLine();

        JClass save = out.currentClass;
        out.currentClass = this;

        printAnnotations (out);
        
        out.printModifiers(modifiers());
        out.print("enum ", name());

        out.print(" {");
        out.indent(1);

        final EnumValue[] values = ecd.getValues();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                out.print(values[i].getNormalizedSymbol());
                if (i != values.length - 1) {
                    out.println(",");
                }
            }

            if (ecd.needsNormalization()) {
                out.println(";");
                // use HashMap for the lookup
                out.println("private final static java.util.HashMap<String,", name(), "> map = new java.util.HashMap<String,", name(), ">();");
                out.print("static {");
                out.indent(1);
                for (EnumValue value : values) {
                    out.println("map.put(\"", value.symbol, "\",", value.getNormalizedSymbol(), ");");
                }
                out.indent(-1);
                out.println("}");
                out.print(String.format("public static %1$s lookup(String value) {\n" +
                        "     final %1$s v = map.get(value);\n" +
                        "     if (v == null) throw new java.lang.IllegalArgumentException(\"No enum const \\\"\" + value + \"\\\" in %1$s\");\n" +
                        "     return v;\n" +
                        "    }", name()));
            }
        }

        out.indent(-1);
        out.println("}");

        out.currentClass = save;

    }
}
