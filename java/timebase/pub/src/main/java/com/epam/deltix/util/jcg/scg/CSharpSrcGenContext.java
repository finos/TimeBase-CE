package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;

import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * Generates C# source code.
 */
public class CSharpSrcGenContext extends JContextImpl {
    public enum AssertMode {
        PROHIBIT,
        NOOP,
        IF_NOT_THROW
    }

    private static final int        MODIFIER_LOCAL_VAR = 0x80000000;

    private AssertMode              assertHandling = AssertMode.NOOP;

    public AssertMode               getAssertMode () {
        return assertHandling;
    }

    public void                     setAssertMode (AssertMode assertHandling) {
        this.assertHandling = assertHandling;
    }

    @Override
    public JStatement               assertStmt (JExpr passCond, JExpr message) {
        switch (assertHandling) {
            case PROHIBIT:
                throw new UnsupportedOperationException ("No assert in C#");

            case NOOP:
                JCompoundStatement  s = compStmt ();
                s.addComment ("assert " + passCond + " : " + message);
                return (s);

            case IF_NOT_THROW:
                return (
                    ifStmt (
                        passCond.not (),
                        newExpr (AssertionError.class, message).throwStmt ()
                    )
                );

            default:
                throw new IllegalStateException (assertHandling.toString ());
        }
    }

    @Override
    public JExpr classLiteral(final Class<?> value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JExpr        classLiteral (JClass value) {
//            return staticCall(/*IKVMUtil.Util*/ , "getFriendlyClassFromType", typeof(value));
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JExpr typeof(final Class<?> type) {
        return new JExprImplBase(this) {
            @Override
            public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                String name = cn(type);
                out.print("typeof(", name, ")");
            }
        };
    }

    @Override
    public JExpr typeof(final JClass type) {
        return new JExprImplBase(this) {
            @Override
            public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                String name = cn(type);
                out.print("typeof(", name, ")");
            }
        };
    }

    @Override
    public JExpr instanceOf(final JExpr arg, final Class<?> toClass) {
        return new JExprImplBase(this) {
            public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                out.print ("(", arg, ") is ", cn1 (toClass));
            }
        };
    }

    @Override
    public JExpr instanceOf(final JExpr arg, final JType toType) {
        return new JExprImplBase(this) {
            public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                out.print ("(", arg, ") is ", cn (toType));
            }
        };
    }

    @Override
    public JExpr staticCall(final Class<?> cls, final String method, final Class<?> typeArgument, final JExpr... args) {
        return (
            new JExprImplBase(this) {
                public void print(int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print(cn(cls), ".", method, "<", cn(typeArgument), ">(");
                    px(out, args);
                    out.print(")");
                }
            }
        );
    }

    @Override
    public JExpr        enumLiteral (final Object value) {
        return (
            new JExprImplBase (this) {
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print(cn1(value.getClass()), '.', value);
                }
            }
        );
    }

    static final JAnnotation OVERRIDE_PSEUDO_ANNOTATION =
        new JAnnotation () { };

    @Override
    public JAnnotation annotation (final Class<?> clazz, final Object... args) {
        if (clazz == Override.class)
            return (OVERRIDE_PSEUDO_ANNOTATION);

        return (
            new JAnnotationImpl () {
                private Object prepare(Object value){
                     return  value instanceof String ? "\"" + value + "\"" : value;
                }
                @Override
                public void print (SourceCodePrinter out) throws IOException {
                    if (args != null && args.length > 0) {
                        if (args.length == 1){
                            out.print ("[", clazz.getName (), "(", prepare (args[0]), ")]");
                        } else {
                            String s = "( ";
                            for (int i = 0; i < args.length; i++) {
                                if ((i + 1) % 2 == 0)
                                    s += prepare(args[i]) + (i == args.length - 1 ? " " : ", ");
                                else
                                    s += args[i] + "=";
                            }
                            s += ")";
                            out.print ("[", clazz.getName (), s, "]");
                        }
                    } else
                        out.print ("[", clazz.getName (),"]");
                }
            }
        );
    }

    @Override
    public JAnnotation annotation (JTypeImpl clazz, Object... args) {
        return (
                new JAnnotationImpl () {
                    private Object prepare(Object value){
                        return  value instanceof String ? "\"" + value + "\"" : value;
                    }
                    @Override
                    public void print (SourceCodePrinter out) throws IOException {
                        if (args != null && args.length > 0) {
                            if (args.length == 1){
                                out.print ("[", clazz.fullName(), "(", prepare (args[0]), ")]");
                            } else {
                                String s = "( ";
                                for (int i = 0; i < args.length; i++) {
                                    if ((i + 1) % 2 == 0)
                                        s += prepare(args[i]) + (i == args.length - 1 ? " " : ", ");
                                    else
                                        s += args[i] + "=";
                                }
                                s += ")";
                                out.print ("[", clazz.fullName (), s, "]");
                            }
                        } else
                            out.print ("[", clazz.fullName (),"]");
                    }
                }
        );
    }

    @Override
    ClassImpl               newClassImpl (int modifiers, String packageName, String simpleName, String parentName) {
        return new CSClassImpl (this, modifiers, packageName, simpleName, parentName);
    }

    @Override
    public String           translateType (String type) {
        switch (type) {
            case "boolean":             return "bool";
            case "boolean[]":           return "bool[]";
            case "java.lang.Object":    return "object";
            case "java.lang.String":    return "string";
            case "java.lang.String[]":  return "string[]";
            case "System.SByte":        return "sbyte";
        }

        return (type);
    }

    @Override
    protected void          printModifiers (int mods, SourceCodePrinter out)
        throws IOException
    {
        if ((mods & Modifier.PUBLIC) != 0)
            out.print("public ");
        else if ((mods & Modifier.PROTECTED) != 0)
            out.print("protected ");
        else if ((mods & Modifier.PRIVATE) != 0)
            out.print("private ");
        else if ((mods & MODIFIER_LOCAL_VAR) == 0) // if NOT local var
            out.print("inner ");

        if ((mods & Modifier.STATIC) != 0)
            out.print("static ");

        if ((mods & Modifier.FINAL) != 0)
            out.print("readonly ");

        // stands for override
        if ((mods & Modifier.INTERFACE) != 0)
            out.print("override ");
    }

    @Override
    protected void          printType (String type, SourceCodePrinter out)
        throws IOException
    {
        out.print (translateType (type));
    }

    @Override
    int                     refineModifiersForLocalVarDecl (int mods) {
        return (MODIFIER_LOCAL_VAR);
    }

    @Override
    int                     refineModifiersForMethodArg (int mods) {
        return (MODIFIER_LOCAL_VAR);
    }
}

