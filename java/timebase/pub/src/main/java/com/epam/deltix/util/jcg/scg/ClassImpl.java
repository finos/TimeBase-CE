package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;

import java.io.IOException;
import java.util.*;

import static com.epam.deltix.util.jcg.scg.JContextImpl.*;

public abstract class ClassImpl
    extends JMemberImpl
    implements JClass, JTypeImpl 
{
    protected final String              fullName;
    protected final String              parentName;
    protected final String              packageName;
    protected final List <String>       interfaceNames = new ArrayList <>();
    final List <JMemberIntf>            members = new ArrayList <> ();
    protected final List <JAnnotation>  annotations = new ArrayList <> ();

    ClassImpl (
        ClassImpl               outer,
        int                     modifiers,
        String                  simpleName,
        String                  parentName
    )
    {
        super (modifiers, simpleName, outer);
        
        this.packageName = outer.packageName ();
        this.fullName = outer.fullName () + "." + simpleName;
        this.parentName = parentName;
    }

    ClassImpl (
        JContextImpl            context,
        int                     modifiers,
        String                  packageName,
        String                  simpleName,
        String                  parentName
    )
    {
        super (context, modifiers, simpleName);

        this.fullName =
            packageName == null || packageName.isEmpty () ?
                simpleName :
                packageName + "." + simpleName;

        this.packageName = packageName;
        this.parentName = parentName;
    }

    @Override
    public String           fullName () {
        return (fullName);
    }

    @Override
    public String           packageName () {
        return (packageName);
    }

    @Override
    public void             addAnnotation (JAnnotation annotation) {
        annotations.add (annotation);
    }
    
    @Override
    public JClass           innerClass (
        int                     modifiers,
        String                  simpleName
    )
    {
        return (innerClass (modifiers, simpleName, (Class <?>) null));
    }

    abstract ClassImpl      innerClassImpl (
        int                     modifiers, 
        String                  simpleName, 
        String                  parentName
    );

    @Override
    public final JClass     innerClass (
        int                     modifiers,
        String                  simpleName,
        Class <?>               parent
    )
    {
        ClassImpl inner = innerClassImpl (modifiers, simpleName, cn (parent));

        members.add (inner);

        return (inner);
    }

    @Override
    public final JClass     innerClass (
        int                     modifiers,
        String                  simpleName,
        JClass                  parent
    )
    {
        ClassImpl inner = innerClassImpl (modifiers, simpleName, cn (parent));

        members.add (inner);

        return (inner);
    }

    @Override
    public void             addImplementedInterface (Class<?> cls) {
        interfaceNames.add (cn (cls));
    }

    @Override
    public void             addImplementedInterface (JClass cls) {
        interfaceNames.add (cn (cls));
    }

    @Override
    public void             addComment (final String text) {
        members.add (
            new JMemberImpl (0, null, this) {
                @Override
                public void printDeclaration (SourceCodePrinter out) throws IOException {
                    for (String s : text.split ("\\n")) {
                        out.newLine ();
                        out.print ("// ", s);
                    }
                }
            }
        );
    }
    
    abstract MethodImpl         createMethod (int modifiers, String typeName, String name);
    
    @Override
    public final JMethod        addMethod (int modifiers, JType type, String name) {
        MethodImpl m = createMethod (modifiers, cn (type), name);

        members.add (m);

        return (m);
    }

    @Override
    public final JMethod        addMethod (int modifiers, Class <?> type, String name) {
        return (addMethod (modifiers, context.classToType (type), name));
    }

    @Override
    public JInitMemberVariable  addVar (int modifiers, Class <?> type, String name) {
        return (addVar (modifiers, type, name, null));
    }

    @Override
    public JInitMemberVariable  addVar (int modifiers, Class <?> type, String name, JExpr initValue) {
        return (addVar (modifiers, context.classToType (type), name, initValue));
    }

    @Override
    public JInitMemberVariable  addVar (int modifiers, JType type, String name) {
        return (addVar (modifiers, type, name, null));
    }

    @Override
    public JInitMemberVariable  addVar (int modifiers, JType type, String name, JExpr initValue) {
        return addVar(modifiers, type, name, initValue, false);
    }

    @Override
    public JInitMemberVariable  addVar (int modifiers, JType type, String name, JExpr initValue, boolean nullable) {
        MemberVariableImpl vdecl =
            new MemberVariableImpl (
                this, modifiers, context.translateType (cn (type)), name);

        if (initValue != null)
            vdecl.setInitValue (initValue);

        members.add (vdecl);

        return (vdecl);
    }

    @Override
    public JMemberVariable addProperty(int modifiers, Class<?> type, String name) {
        throw new UnsupportedOperationException("Not supported in Java");
    }

    @Override
    public JMemberVariable      inheritedVar (String name) {
        return (new MemberVariableImpl (context, 0, null, name));
    }

    @Override
    public JMemberVariable      thisVar () {
        return (new ThisVariableImpl (this));
    }

    @Override
    public JExpr            newExpr (final JExpr ... args) {
        return (
            new JExprImplBase (context) {
                @Override
                public void     print (int outerPriority, SourceCodePrinter out) throws IOException {
                    out.print ("new ", fullName, " (");
                    px (out, args);
                    out.print (")");
                }
            }
        );
    }

    abstract ConstructorImpl    newConstructor (int modifiers);
        
    @Override
    public final JConstructor     addConstructor (int modifiers) {
        ConstructorImpl m = newConstructor (modifiers);
        members.add (m);
        return (m);
    }

    public JConstructor     getConstructor() {
        for (JMemberIntf member : members) {
            if (member instanceof ConstructorImpl)
                return (JConstructor) member;
        }

        throw new RuntimeException("there is no constructor defined");
    }
    
    protected void          printAnnotations (SourceCodePrinter out) throws IOException {    
        if ( annotations.size () > 0) {
            out.newLine ();
            for (JAnnotation annotation : annotations) {
                out.print (annotation);
                out.newLine ();
            }
        }
    }

    public JMethod getMethod(String name) {
        for (JMemberIntf statement : members) {
            if (statement instanceof JMethod && name.equals(statement.name()))
                return (JMethod)statement;
        }

        return null;
    }

    @Override
    public List<JMemberVariable> getVars() {
        ArrayList<JMemberVariable> vars = new ArrayList<>();

        for (JMemberIntf member : members) {
            if (member instanceof JMemberVariable)
                vars.add((JMemberVariable) member);
        }

        return vars;
    }

    @Override
    public  JMemberVariable               getVar(String name) {
        for (JMemberIntf statement : members) {
            if (statement instanceof JMemberVariable && name.equals(statement.name()))
                return (JMemberVariable)statement;
        }

        return null;
    }
}
