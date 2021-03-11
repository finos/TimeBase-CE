package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

/**
 *  Generates Java source code.
 */
public class JavaSrcGenContext extends JContextImpl {
    @Override
    public JExpr classLiteral(Class<?> value) {
        return staticVarRef(value, "class");
    }

    @Override
    public JExpr        classLiteral (JClass value) {
        return staticVarRef(value, "class");
    }

    @Override
    public JStatement assertStmt(final JExpr passCond, final JExpr message) {
        return (
            new JStatementImplBase (this) {
                @Override
                public void     printElement (SourceCodePrinter out) throws IOException {
                    out.print ("assert ", passCond);
                    
                    if(message != null)
                        out.print(" : ", message);

                    out.print (";");
                }
            }
        );
    }

    @Override
    public JExpr instanceOf(final JExpr arg, final Class<?> toClass) {
        return new JExprImplBase(this) {
            @Override
            public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                printPostfix (                    
                    outerPriority, 
                    (JExprImplBase) arg, 
                    " instanceof " + cn1 (toClass),
                    JavaOpPriority.RELATIONAL,
                    out
                );
            }
        };
    }

    @Override
    public JExpr instanceOf(final JExpr arg, final JType toType) {
        return new JExprImplBase(this) {
            @Override
            public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                printPostfix (                    
                    outerPriority, 
                    (JExprImplBase) arg, 
                    " instanceof " + cn (toType),
                    JavaOpPriority.RELATIONAL,
                    out
                );
            }
        };
    }

    @Override
    public JExpr typeof(Class<?> type) {
        throw new UnsupportedOperationException(
            "Java has no typeof operation"
        );
    }

    @Override
    public JExpr typeof(JClass type) {
        throw new UnsupportedOperationException(
            "Java has no typeof operation"
        );
    }

    @Override
    public JExpr staticCall(Class<?> cls, String method, Class<?> typeArgument, JExpr... args) {
        throw new UnsupportedOperationException(
            "Java does't support typeArgument in a generic function call"
        );
    }

    @Override
    public JExpr        enumLiteral (Object value) {
        throw new UnsupportedOperationException(
                "This override is for .NET only"
        );
    }

    @Override
    public JAnnotation annotation (final Class<?> clazz, final Object... args) {
        return (
            new JAnnotationImpl () {

                private Object prepare(Object value){
                     return  value instanceof String ? "\"" + value + "\"" : value;
                }
                @Override
                public void print (SourceCodePrinter out) throws IOException {
                    if (args != null && args.length > 0) {
                        if (args.length == 1){
                            out.print ("@", clazz.getName (), "(", prepare (args[0]), ")");
                        } else {
                            String s = "( ";
                            for (int i = 0; i < args.length; i++) {
                                if ((i + 1) % 2 == 0)
                                    s += prepare(args[i]) + (i == args.length - 1 ? " " : ", ");
                                else
                                    s += args[i] + "=";
                            }
                            s += ")";
                            out.print ("@", clazz.getName (), s);
                        }
                    } else
                        out.print ("@", clazz.getName ());
                }
            }
        );

    }

    @Override
    public JAnnotation annotation (JTypeImpl clazz, Object... args) {
        throw new UnsupportedOperationException("Unsupported for java.");
    }

    @Override
    ClassImpl               newClassImpl (int modifiers, String packageName, String simpleName, String parentName) {
        return new JClassImpl (this, modifiers, packageName, simpleName, parentName);
    }
    
    @Override
    public String           translateType(String type) {
        return type;
    }
        
    @Override
    protected void          printModifiers (int mods, SourceCodePrinter out) 
        throws IOException 
    {
        out.printModifiers (mods);
    }

    @Override
    protected void printType(String type, SourceCodePrinter out)
            throws IOException {

        String[] primitiveTypes = new String[]{
                "boolean",
                "char",
                "byte",
                "short",
                "int",
                "long",
                "float",
                "double"
        };
        HashSet<String> ignoreSet = new HashSet<>(Arrays.asList(primitiveTypes));
        ignoreSet.add("void");

        for (String t : primitiveTypes) {
            ignoreSet.add(t + "[]");
        }
        if (ignoreSet.contains(type)) {
            out.print(type);
        } else {
            out.printRefClassName(type);
        }
    }
    
           
}
