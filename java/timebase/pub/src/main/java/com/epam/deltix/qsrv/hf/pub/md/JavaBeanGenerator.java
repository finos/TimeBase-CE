package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.jcg.scg.*;
import com.epam.deltix.util.lang.CompilationExceptionWithDiagnostic;
import com.epam.deltix.util.lang.JavaCompilerHelper;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 *  Generates and optionally compiles/loads classes corresponding to specified
 *  TimeBase class descriptors.
 */
public class JavaBeanGenerator extends BeanGenerator {
    private static final boolean        DUMP_SOURCES = false;

    private static final ClassLoader    CL =
        InstrumentMessage.class.getClassLoader ();

    private enum PState {
        UNPROCESSED,
        WIP,
        PROCESSED
    }

    public static enum Language {
        JAVA,
        CS
    }

    public class Bean {
        public final Bean               parent;
        public final ClassDescriptor    cd;
        private final String            pack;
        private final String            className;
        private Set <String>            ignoreFields = null;
        private PState                  state = PState.UNPROCESSED;
        private ClassImpl               cimpl = null;
        private Class <?>               cls = null;

        Bean (Bean parent, ClassDescriptor cd, String pack, String className, Class <?> cls) {
            this.parent = parent;
            this.cd = cd;
            this.pack = pack;
            this.className = className;
            this.cls = cls;
        }

        public boolean      isIgnored (String fieldName) {
            return (ignoreFields != null && ignoreFields.contains (fieldName));
        }

        public void         ignoreFields (String ... names) {
            if (ignoreFields == null)
                ignoreFields = new HashSet <> (names.length);

            for (String name : names)
                ignoreFields.add (name);
        }

        public String       getSchemaClassName () {
            return (cd.getName ());
        }

        public String       getNativeClassName () {
            if (pack.length () == 0)
                return (className);

            return (pack + "." + className);
        }

        public String       getSourceCode () {
            if (state != PState.PROCESSED)
                throw new IllegalStateException (state.name ());

            NiceSourceCodePrinter   out;
            //String                  qql = GrammarUtil.describe ("", cd);

            switch (lang) {
                case JAVA: {
                    JavaCodePrinter         jcp = new JavaCodePrinter (pack);

                    jcp.setImportMode (JavaCodePrinter.ImportMode.BY_CLASS);
                    //jcp.setTopComment ("Message object for:\n" + qql);

                    out = jcp;
                    break;
                }

                case CS: {
                    CSCodePrinter           cscp = new CSCodePrinter (pack);

                    cscp.setImportMode (CSCodePrinter.ImportMode.BY_NAMESPACE);
                    //cscp.setTopComment ("Message object for:\n<pre>" + qql + "</pre>");

                    out = cscp;
                    break;
                }

                default:
                    throw new UnsupportedOperationException (lang.name ());
            }

            try {
                cimpl.printDeclaration (out);
                return (out.getSourceCode ());
            } catch (IOException x) {
                throw new RuntimeException (x);
            }
        }
    }

    private final HashMap <ClassDescriptor, Bean>   beans =
        new HashMap <> ();

    private final HashMap <String, Bean>            beansByNativeClassName =
        new HashMap <> ();

    private final HashMap <String, Bean>            beansBySchemaClassName =
        new HashMap <> ();

    private final JContextImpl          cgx;
    private boolean                     regenExistingClasses = true;
    private String                      defaultPackage = "";
    private final Language              lang;
    private boolean                     longAlphanumerics = true;

    public JavaBeanGenerator (Language lang) {
        switch (lang) {
            case JAVA:  cgx = new JavaSrcGenContext (); break;
            case CS:    cgx = new CSharpSrcGenContext (); break;
            default:    throw new IllegalArgumentException (lang.name ());
        }

        this.lang = lang;
    }

    public JavaBeanGenerator () {
        this (Language.JAVA);
    }

    public Language         getLanguage () {
        return lang;
    }

    public boolean          getLongAlphanumerics () {
        return longAlphanumerics;
    }

    /**
     *  Whether to map VARCHAR (ALPHANUMERIC) to long.
     *
     *  @param longAlphanumerics
     *              If true, <code>VARCHAR (ALPHANUMERIC)</code> will be mapped to
     *              a <code>long</code> native data type.
     */
    public void             setLongAlphanumerics (boolean longAlphanumerics) {
        this.longAlphanumerics = longAlphanumerics;
    }

    public boolean          getRegenExistingClasses () {
        return regenExistingClasses;
    }

    public void             setRegenExistingClasses (boolean b) {
        this.regenExistingClasses = b;
    }

    public String           getDefaultPackage () {
        return defaultPackage;
    }

    public void             setDefaultPackage (String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }


    protected String        getClassNameFor (ClassDescriptor cd) {
        String          cdName = cd.getName ();
        int             dot = cdName.lastIndexOf ('.');

        return (lang == Language.JAVA) ?
                (escapeIdentifierForJava (dot < 0 ? cdName : cdName.substring (dot + 1))) :
                (escapeIdentifierForCS (dot < 0 ? cdName : cdName.substring (dot + 1)));
    }

    protected Class <?>     loadExternalClass (ClassDescriptor cd) {
        if (regenExistingClasses)
            return (null);

        try {
            return (CL.loadClass (cd.getName ()));
        } catch (ClassNotFoundException x) {
            return (null);
        }
    }

    public final Bean       addClass (ClassDescriptor cd) {
        Bean    ret = beans.get (cd);

        if (ret == null) {
            Bean    pbean = null;

            if (cd instanceof RecordClassDescriptor) {
                RecordClassDescriptor   rcd = (RecordClassDescriptor) cd;
                RecordClassDescriptor   prcd = rcd.getParent ();

                if (prcd != null)
                    pbean = addClass (prcd);
            }

            Class <?>   cls = loadExternalClass (cd);
            String      pack;
            String      sname;

            if (cls == null) {
                sname = getClassNameFor (cd);
                pack = defaultPackage;
            }
            else {
                sname = cls.getSimpleName ();
                pack = cls.getName ();

                int     n = pack.lastIndexOf ('.');
                if (n < 0)
                    pack = "";
                else
                    pack = pack.substring (0, n);
            }

            ret = new Bean (pbean, cd, pack, sname, cls);

            beans.put (cd, ret);
            beansByNativeClassName.put (ret.getNativeClassName (), ret);
            beansBySchemaClassName.put (ret.getSchemaClassName (), ret);
        }

        return (ret);
    }

    public final Bean       getByNativeClassName (String name) {
        return (beansByNativeClassName.get (name));
    }

    public final Bean       getBySchemaClassName (String name) {
        return (beansBySchemaClassName.get (name));
    }

//    public static String    escapeIdentifierForJava (String id) {
//        if (StringUtils.isJavaReservedWord (id))
//            return (escapeJavaReservedWord (id));
//
//        if (StringUtils.isValidJavaIdOrKeyword (id))
//            return (id);
//
//        return (doEscapeIdentifierForJava (id));
//    }

//    public static String    escapeIdentifierForCS (String id) {
//        if (StringUtils.isCSReservedWord (id))
//            return ("@" + id);
//
//        if (StringUtils.isValidCSIdOrKeyword (id))
//            return (id);
//
//        return (doEscapeIdentifierForCS (id));
//    }

//    public static String    doEscapeIdentifierForJava (String id) {
//        final StringBuilder     sb = new StringBuilder ();
//        final int               fnameLength = id.length ();
//
//        for (int ii = 0; ii < fnameLength; ii++) {
//            char    c = id.charAt (ii);
//
//            if (c == '$')
//                sb.append ("$$");
//            else if (ii == 0 ?
//                        !Character.isJavaIdentifierStart (c) :
//                        !Character.isJavaIdentifierPart (c))
//            {
//                sb.append ("$");
//                sb.append ((int) c);
//                sb.append ("$");
//            }
//            else
//                sb.append (c);
//        }
//
//        return (sb.toString ());
//    }

//    public static String    doEscapeIdentifierForCS (String id) {
//        final StringBuilder     sb = new StringBuilder ();
//        final int               n = id.length ();
//
//        for (int ii = 0; ii < n; ii++) {
//            char    c = id.charAt (ii);
//
//            if (c == '_')
//                sb.append ("__");
//            else if (ii == 0 ?
//                        !StringUtils.isCSIdentifierStart (c) :
//                        !StringUtils.isCSIdentifierPart (c))
//            {
//                sb.append ("_");
//                sb.append ((int) c);
//                sb.append ("_");
//            }
//            else
//                sb.append (c);
//        }
//
//        return (sb.toString ());
//    }

    public static String escapeJavaReservedWord (String keyword){
        return "$" + keyword;
    }

    private static final String     NAME = "name";

    private ClassImpl processJavaEnum(
            String packName,
            String simpleName,
            EnumClassDescriptor ecd
    )
    {
        JEnumImpl cimpl = new JEnumImpl(cgx, Modifier.PUBLIC, packName, simpleName, null, ecd);

        if (lang == Language.JAVA)
            cimpl.addAnnotation (cgx.annotation (SchemaElement.class, "name", ecd.getName ()));
        else
            cimpl.addAnnotation (cgx.annotation (((JTypeImpl) () -> "Deltix.Timebase.Api.SchemaElement"), "Name", ecd.getName ()));

        return cimpl;
    }

    private ClassImpl       processRecord (Bean bean) {
        if (bean.parent != null)
            process (bean.parent);

        String                  packName = bean.pack;
        String                  simpleName = bean.className;
        RecordClassDescriptor   rcd = (RecordClassDescriptor) bean.cd;

        int             mods = Modifier.PUBLIC;

        if (rcd.isAbstract ())
            mods |= Modifier.ABSTRACT;

        ClassImpl       cimpl;

        if (bean.parent == null)
            cimpl = cgx.newClass (mods, packName, simpleName, InstrumentMessage.class);
        else if (bean.parent.cimpl == null)
            cimpl = cgx.newClass (mods, packName, simpleName, bean.parent.cls);
        else
            cimpl = cgx.newClass (mods, packName, simpleName, bean.parent.cimpl);

        if (lang == Language.JAVA)
            cimpl.addAnnotation (cgx.annotation (SchemaElement.class, "name", rcd.getName ()));
        else
            cimpl.addAnnotation (cgx.annotation (((JTypeImpl) () -> "Deltix.Timebase.Api.SchemaElement"), "Name", rcd.getName ()));

//        if (rcd.isConvertibleTo (GenericInstrument.class.getName ())) {
//            final String javaClassName = getName (rcd);
//            if ( ! StringUtils.isEmpty(javaClassName)) {
//                InstrumentType instrumentType = GenericInstrumentHelper.getInstrumentType(javaClassName);
//                JConstructor cons = cimpl.addConstructor (Modifier.PUBLIC);
//                cons.body ().add (  cimpl.inheritedVar ((lang == Language.JAVA) ? "instrumentType" : "InstrumentType")
//                        .access ().assign ((lang == Language.JAVA) ?
//                                cgx.staticVarRef (InstrumentType.class, instrumentType.toString ()) :
//                                cgx.staticVarRef ("Deltix.Timebase.Api.Messages.InstrumentType", toCSEnum(instrumentType.toString ()))
//                ));
//            }
//        }

        for (DataField df : rcd.getFields ()) {
            DataType        ftype = df.getType ();
            final String    fname = df.getName ();

            if (bean.ignoreFields != null && bean.ignoreFields.contains (fname))
                continue;

            String          fjname =  (lang == Language.JAVA) ?
                    escapeIdentifierForJava (fname) :
                    escapeIdentifierForCS(fname);

            JInitMemberVariable variable;

            if (ftype instanceof ArrayDataType) {

                variable = (lang == Language.JAVA) ?
                        createJavaVar(
                                fjname,
                                ((ArrayDataType) ftype).getElementDataType(),
                                df,
                                bean,
                                cimpl,
                                true
                        ) :
                        createCSVar(
                                fjname,
                                ((ArrayDataType) ftype).getElementDataType(),
                                df,
                                bean,
                                cimpl,
                                true
                        );
            } else {
                variable = (lang == Language.JAVA) ?
                        createJavaVar(
                                fjname,
                                ftype,
                                df,
                                bean,
                                cimpl,
                                false
                        ) :
                        createCSVar(
                                fjname,
                                ftype,
                                df,
                                bean,
                                cimpl,
                                false
                        );
            }

            if (variable != null && !fjname.equals (fname)) {
                if (lang == Language.JAVA)
                    variable.addAnnotation (cgx.annotation (SchemaElement.class, "name", fname));
                else
                    variable.addAnnotation (cgx.annotation (((JTypeImpl) () -> "Deltix.Timebase.Api.SchemaElement"), "Name", fname));
            }
        }

        // create copy methods and toString

        if (rcd.getFields().length > 0) {
            JMethod             copyFrom;

            if (lang == Language.JAVA)
                copyFrom = cimpl.addMethod (Modifier.PUBLIC, InstrumentMessage.class, "copyFrom");
            else
                copyFrom = cimpl.addMethod(Modifier.PROTECTED, ((JTypeImpl) () -> "Deltix.Timebase.Api.Messages.IRecordInterface"), "CopyFromImpl");

            copyFrom.addAnnotation(cgx.annotation(Override.class));

            JMethodArgument     source = (lang == Language.JAVA) ?
                    copyFrom.addArg(0, RecordInfo.class, "source") :
                    copyFrom.addArg(0, ((JTypeImpl) () -> "Deltix.Timebase.Api.Messages.IRecordInfo"), "source");

            JMethod             toString =
                cimpl.addMethod (Modifier.PUBLIC, String.class, (lang == Language.JAVA) ? "toString" : "ToString");

            toString.addAnnotation(cgx.annotation(Override.class));

            JCompoundStatement  copyFromBody = copyFrom.body();
            JExpr               tsReturn = cimpl.callSuperMethod ((lang == Language.JAVA) ? "toString" : "ToString");

            copyFromBody.add(cimpl.callSuperMethod((lang == Language.JAVA) ? "copyFrom" : "CopyFrom", source));

            JCompoundStatement ifs = cgx.compStmt();
            JLocalVariable var = ifs.addVar(Modifier.FINAL, cimpl, "obj", source.cast(cimpl));
            for (DataField df : rcd.getFields ()) {
                String          dfname = df.getName();

                if (bean.ignoreFields != null && bean.ignoreFields.contains (dfname))
                    continue;

                if (df instanceof NonStaticDataField) {
                    String          fieldName = (lang == Language.JAVA) ?
                            escapeIdentifierForJava (dfname) :
                            escapeIdentifierForCS(dfname);
                    JExpr           mv = cimpl.inheritedVar (fieldName).access ();

                    tsReturn = cgx.binExpr (tsReturn, "+", cgx.stringLiteral (", "));
                    tsReturn = cgx.binExpr (tsReturn, "+", cgx.stringLiteral (dfname));
                    tsReturn = cgx.binExpr (tsReturn, "+", cgx.stringLiteral (": "));
                    tsReturn = cgx.binExpr (tsReturn, "+", mv);

                    if (!(df.getType() instanceof BinaryDataType)) {
                        ifs.add (mv.assign (var.field (fieldName)));
                    } else {
                        ifs.add (mv.call((lang == Language.JAVA) ? "clear" : "Clear"));
                        ifs.add (mv.call((lang == Language.JAVA) ? "addAll" : "Append", var.field (fieldName)));
                    }
                }
            }

            copyFromBody.add(cgx.ifStmt(cgx.instanceOf(source, cimpl), ifs));
            copyFromBody.add(cimpl.thisVar().access().returnStmt());
            toString.body ().add (tsReturn.returnStmt ());
        }

        if (!rcd.isAbstract()) {
            JMethod clone;

            if (lang == Language.JAVA)
                clone = cimpl.addMethod (Modifier.PUBLIC, InstrumentMessage.class, "clone");
            else
                clone = cimpl.addMethod(Modifier.PROTECTED, ((JTypeImpl) () -> "Deltix.Timebase.Api.Messages.IRecordInfo"), "CloneImpl");

            clone.addAnnotation(cgx.annotation(Override.class));
            JCompoundStatement body = clone.body();

            JLocalVariable msg = body.addVar(Modifier.FINAL, cimpl, "msg", cgx.newExpr(cimpl));
            body.add(msg.call((lang == Language.JAVA) ? "copyFrom" : "CopyFrom", cimpl.thisVar().access()));
            body.add(msg.returnStmt());
        }

        return (cimpl);
    }

    private JInitMemberVariable createJavaVar(final String    fjname,
                                              final DataType  ftype,
                                              final DataField df,
                                              final Bean      bean,
                                              final ClassImpl cimpl,
                                              final boolean   isArray) {
        JInitMemberVariable variable = null;

        if (ftype instanceof ClassDataType) {
            RecordClassDescriptor[] descriptors = ((ClassDataType)ftype).getDescriptors();
            for (RecordClassDescriptor r: descriptors){
                addAndProcess(r);
            }
            variable = cimpl.addVar(
                    Modifier.PUBLIC,
                    isArray ? ObjectArrayList.class : InstrumentMessage.class ,
                    fjname);
            if (isArray) {
                variable.addAnnotation((JAnnotationImpl) out -> {
                    out.print('@'); out.print(SchemaArrayType.class.getName());
                    out.print("( elementDataType = ");
                    out.print(SchemaDataType.class.getName());
                    out.print('.');
                    out.print(SchemaDataType.OBJECT.toString());
                    out.print(", elementTypes = { ");
                    writeNestedClassesJava(out, descriptors);
                    out.print("})");
                }); //[SchemaArrayType]
            } else {
                variable.addAnnotation((JAnnotationImpl) out -> {
                    out.print('@'); out.print(SchemaType.class.getName());
                    out.print("( dataType = ");
                    out.print(SchemaDataType.class.getName());
                    out.print('.');
                    out.print(SchemaDataType.OBJECT.toString());
                    out.print(", nestedTypes = { ");
                    writeNestedClassesJava(out, descriptors);
                    out.print("})");
                }); //[SchemaType]
            }
        } else if (ftype instanceof EnumDataType) {
            EnumDataType edt = (EnumDataType) ftype;
            //Don't generate enum class, if there is java enum
            try {
                Class<?> clazz = CL.loadClass(edt.getBaseName());
                variable = cimpl.addVar(Modifier.PUBLIC,
                        isArray ? ObjectArrayList.class : clazz,
                        fjname);
            } catch (ClassNotFoundException e) {
                //there is no exist java enum, we need to generate code
                ClassImpl ecimpl = addAndProcess(edt.getDescriptor());
                variable = cimpl.addVar(Modifier.PUBLIC, ecimpl, fjname, null, edt.isNullable());
            }
        } else if (ftype instanceof BooleanDataType) {
            if (ftype.isNullable())
                variable = cimpl.addVar(
                        Modifier.PUBLIC,
                        isArray ? ByteArrayList.class : byte.class,
                        fjname,
                        isArray ? null : cgx.staticVarRef(BooleanDataType.class, "NULL")
                );
            else
                cimpl.addVar(Modifier.PUBLIC, byte.class, fjname);
        } else if (ftype instanceof IntegerDataType) {
            IntegerDataType idt = (IntegerDataType) ftype;
            int size = idt.getSize();
            Class<?> fc;
            String nullName = "INT" + size * 8 + "_NULL";

            switch (size) {
                case 1:
                    fc = isArray ? ByteArrayList.class : byte.class;
                    break;

                case 2:
                    fc = isArray ? ShortArrayList.class : short.class ;
                    break;

                case 4:
                    fc = isArray ? IntegerArrayList.class : int.class;
                    break;

                case 6:
                case 8:
                    fc = isArray ? LongArrayList.class : long.class;
                    break;

                case IntegerDataType.PACKED_UNSIGNED_INT:
                    fc = isArray ? IntegerArrayList.class : int.class;
                    nullName = "PUINT30_NULL";
                    break;

                case IntegerDataType.PACKED_UNSIGNED_LONG:
                    fc = isArray ? LongArrayList.class : long.class;
                    nullName = "PUINT61_NULL";
                    break;

                case IntegerDataType.PACKED_INTERVAL:
                    fc = isArray ? IntegerArrayList.class : int.class;
                    nullName = "PINTERVAL_NULL";
                    break;

                default:
                    throw new UnsupportedOperationException("INTEGER size: " + size);
            }

            variable =
                    cimpl.addVar(
                            Modifier.PUBLIC,
                            fc,
                            fjname,
                            isArray ? null : cgx.staticVarRef(IntegerDataType.class, nullName) );
        } else if (ftype instanceof FloatDataType) {
            FloatDataType fdt = (FloatDataType) ftype;

            if (fdt.isFloat())
                variable =
                        cimpl.addVar(
                                Modifier.PUBLIC,
                                isArray ? FloatArrayList.class : float.class,
                                fjname,
                                isArray ? null : cgx.staticVarRef(FloatDataType.class, "IEEE32_NULL")
                        );
            else
                variable =
                        cimpl.addVar(
                                Modifier.PUBLIC,
                                isArray ? DoubleArrayList.class : double.class,
                                fjname,
                                isArray ? null : cgx.staticVarRef(FloatDataType.class, "IEEE64_NULL")
                        );
        } else if (ftype instanceof TimeOfDayDataType)
            variable =
                    cimpl.addVar(
                            Modifier.PUBLIC,
                            isArray ? IntegerArrayList.class : int.class,
                            fjname,
                            isArray ? null : cgx.staticVarRef(TimeOfDayDataType.class, "NULL")
                    );
        else if (ftype instanceof CharDataType)
            variable =
                    cimpl.addVar(
                            Modifier.PUBLIC,
                            isArray ? CharacterArrayList.class : char.class,
                            fjname,
                            isArray ? null : cgx.staticVarRef(CharDataType.class, "NULL")
                    );
        else if (ftype instanceof DateTimeDataType)
            variable = cimpl.addVar(
                    Modifier.PUBLIC,
                    isArray ? LongArrayList.class : long.class,
                    fjname,
                    isArray ? null : cgx.staticVarRef(DateTimeDataType.class, "NULL")
            );
        else if (ftype instanceof VarcharDataType) {
            if (longAlphanumerics &&
                    ((VarcharDataType) ftype).getEncodingType() == VarcharDataType.ALPHANUMERIC)
                variable = cimpl.addVar(
                        Modifier.PUBLIC,
                        isArray ? LongArrayList.class : long.class,
                        fjname,
                        isArray ? null : cgx.staticVarRef(IntegerDataType.class, "INT64_NULL") // HACK!!! TODO: FIX
                );
            else if (lang == Language.JAVA)
                variable = cimpl.addVar(
                        Modifier.PUBLIC,
                        isArray ? ObjectArrayList.class : String.class,
                        fjname
                );
            else
                variable = cimpl.addVar(
                        Modifier.PUBLIC,
                        isArray ? ObjectArrayList.class : String.class,
                        fjname
                );
        } else if (ftype instanceof BinaryDataType) {
            if (bean.ignoreFields != null && bean.ignoreFields.contains(df.getName()))
                return null;
            if (ftype.isNullable())
                cimpl.addVar(
                        Modifier.PUBLIC,
                        isArray ? BooleanArrayList.class : boolean.class,
                        fjname + (lang == Language.JAVA ? "$" : "__") + "IsNull",
                        isArray ? null : cgx.falseLiteral()
                );

            variable = cimpl.addVar(
                    Modifier.PUBLIC,// | Modifier.FINAL,
                    isArray ? ObjectArrayList.class :
                            deltix.util.collections.generated.ByteArrayList.class,
                    fjname,
                    isArray ? null : cgx.newExpr(deltix.util.collections.generated.ByteArrayList.class)
            );
        }

        if (variable != null)
            variable.addComment(df.getDescription());

        return variable;
    }

    private void writeNestedClassesJava (SourceCodePrinter out, RecordClassDescriptor[] descriptors) throws IOException {
        for (int i = 0; i < descriptors.length; i++){
            Class <?>   cls = loadExternalClass (descriptors[i]);
            String      sname = (cls == null) ?
                    getClassNameFor (descriptors[i]) :
                    cls.getSimpleName ();

            out.print(sname);
            out.print(".class ");
            out.print((i < descriptors.length - 1) ? ", " : " ");
        }
    }

    private void writeNestedClassesCS (SourceCodePrinter out, RecordClassDescriptor[] descriptors) throws IOException {
        for (int i = 0; i < descriptors.length; i++){
            out.print("typeof(");
            Class <?>   cls = loadExternalClass (descriptors[i]);
            String      sname = (cls == null) ?
                    getClassNameFor (descriptors[i]) :
                    cls.getSimpleName ();

            out.print(sname);
            out.print((i < descriptors.length - 1) ? "), " : ") ");
        }
    }

    private JInitMemberVariable createCSVar(final String    fjname,
                                              final DataType  ftype,
                                              final DataField df,
                                              final Bean      bean,
                                              final ClassImpl cimpl,
                                              final boolean   isArray) {
        JInitMemberVariable variable = null;

        if (ftype instanceof ClassDataType) {
            RecordClassDescriptor[] descriptors = ((ClassDataType)ftype).getDescriptors();
            for (RecordClassDescriptor r: descriptors){
                addAndProcess(r);
            }
            variable = cimpl.addVar(
                    Modifier.PUBLIC,
                    isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<object>") :
                            ((JTypeImpl) () -> "object"),
                    fjname);

            if (isArray) {
                variable.addAnnotation((JAnnotationImpl) out -> {
                    out.print("[Deltix.Timebase.Api.SchemaArrayType(ElementDataType = Deltix.Timebase.Api.SchemaDataType.Object, ElementTypes = new[] { ");
                    writeNestedClassesCS(out, descriptors);
                    out.print("})]");
                }); //[SchemaArrayType]
            } else {
                variable.addAnnotation((JAnnotationImpl) out -> {
                    out.print("[Deltix.Timebase.Api.SchemaType(DataType = Deltix.Timebase.Api.SchemaDataType.Object, NestedTypes = new[] { ");
                    writeNestedClassesCS(out, descriptors);
                    out.print("})]");
                }); //[SchemaType]
            }

        } else if (ftype instanceof EnumDataType) {
            EnumDataType edt = (EnumDataType) ftype;
            //Don't generate enum class, if there is java enum
            try {
                Class<?> clazz = CL.loadClass(edt.getBaseName());
                variable = cimpl.addVar(Modifier.PUBLIC,
                        isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<object>") :
                                ((JTypeImpl) () -> toCSNamespace(clazz.getName())),
                        fjname);
            } catch (ClassNotFoundException e) {
                //there is no exist java enum, we need to generate code
                ClassImpl ecimpl = addAndProcess(edt.getDescriptor());
                variable = cimpl.addVar(Modifier.PUBLIC, ecimpl, fjname, null, edt.isNullable());
            }
        } else if (ftype instanceof BooleanDataType) {
            if (ftype.isNullable())
                variable = cimpl.addVar(
                        Modifier.PUBLIC,
                        isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<sbyte>") :
                                ((JTypeImpl) () -> "sbyte"),
                        fjname,
                        isArray ? null : cgx.staticVarRef("Deltix.Timebase.Api.Schema.Types.BooleanDataType", "Null")
                );
            else
                cimpl.addVar(Modifier.PUBLIC, ((JTypeImpl) () -> "sbyte"), fjname);
        } else if (ftype instanceof IntegerDataType) {
            IntegerDataType idt = (IntegerDataType) ftype;
            int size = idt.getSize();
            JType ft;
            String nullName = "Int" + size * 8 + "Null";

            switch (size) {
                case 1:
                    ft = isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<sbyte>") : ((JTypeImpl) () -> "sbyte");
                    break;

                case 2:
                    ft = isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<short>") : ((JTypeImpl) () -> "short");
                    break;

                case 4:
                    ft = isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<int>") : ((JTypeImpl) () -> "int");
                    break;

                case 6:
                case 8:
                    ft = isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<long>") : ((JTypeImpl) () -> "long");
                    break;

                case IntegerDataType.PACKED_UNSIGNED_INT:
                    ft = isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<uint>") : ((JTypeImpl) () -> "uint");
                    nullName = "Puint30Null";
                    break;

                case IntegerDataType.PACKED_UNSIGNED_LONG:
                    ft = isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<long>") : ((JTypeImpl) () -> "long");
                    nullName = "Puint61Null";
                    break;

                case IntegerDataType.PACKED_INTERVAL:
                    ft = isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<int>") : ((JTypeImpl) () -> "int");
                    nullName = "PintervalNull";
                    break;

                default:
                    throw new UnsupportedOperationException("INTEGER size: " + size);
            }

            variable =
                    cimpl.addVar(
                            Modifier.PUBLIC,
                            ft,
                            fjname,
                            isArray ? null : cgx.staticVarRef("Deltix.Timebase.Api.Schema.Types.IntegerDataType", nullName)
                    );
        } else if (ftype instanceof FloatDataType) {
            FloatDataType fdt = (FloatDataType) ftype;

            if (fdt.isFloat())
                variable =
                        cimpl.addVar(
                                Modifier.PUBLIC,
                                isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<float>") : ((JTypeImpl) () -> "float"),
                                fjname,
                                isArray ? null : cgx.staticVarRef("Deltix.Timebase.Api.Schema.Types.FloatDataType", "Ieee32Null")
                        );
            else
                variable =
                        cimpl.addVar(
                                Modifier.PUBLIC,
                                isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<double>") : ((JTypeImpl) () -> "double"),
                                fjname,
                                isArray ? null : cgx.staticVarRef("Deltix.Timebase.Api.Schema.Types.FloatDataType", "Ieee64Null")
                        );
        } else if (ftype instanceof TimeOfDayDataType)
            variable =
                    cimpl.addVar(
                            Modifier.PUBLIC,
                            isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<System.TimeSpan>") : ((JTypeImpl) () -> "System.TimeSpan"),
                            fjname,
                            isArray ? null : cgx.staticVarRef("Deltix.Timebase.Api.Schema.Types.TimeOfDayDataType", "TimeSpanNull")
                    );
        else if (ftype instanceof CharDataType)
            variable =
                    cimpl.addVar(
                            Modifier.PUBLIC,
                            isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<char>") : ((JTypeImpl) () -> "char"),
                            fjname,
                            isArray ? null : cgx.staticVarRef("Deltix.Timebase.Api.Schema.Types.CharDataType", "Null")
                    );
        else if (ftype instanceof DateTimeDataType)
            variable = cimpl.addVar(
                    Modifier.PUBLIC,
                    isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<long>") : ((JTypeImpl) () -> "long"),
                    fjname,
                    isArray ? null : cgx.staticVarRef("Deltix.Timebase.Api.Schema.Types.DateTimeDataType", "LongNull")
            );
        else if (ftype instanceof VarcharDataType) {
            if (longAlphanumerics &&
                    ((VarcharDataType) ftype).getEncodingType() == VarcharDataType.ALPHANUMERIC)
                variable = cimpl.addVar(
                        Modifier.PUBLIC,
                        isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<long>") : ((JTypeImpl) () -> "long"),
                        fjname,
                        isArray ? null :cgx.staticVarRef("Deltix.Timebase.Api.Schema.Types.IntegerDataType", "Int64Null") // HACK!!! TODO: FIX
                );
            else
                variable = cimpl.addVar(
                        Modifier.PUBLIC,
                        isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<string>") : ((JTypeImpl) () -> "string"),
                        fjname
                );
        } else if (ftype instanceof BinaryDataType) {
            if (bean.ignoreFields != null && bean.ignoreFields.contains(df.getName()))
                return null;
            if (ftype.isNullable())
                cimpl.addVar(
                        Modifier.PUBLIC,
                        isArray ? ((JTypeImpl) () -> "System.Collections.Generic.IList<bool>") : ((JTypeImpl) () -> "bool"),
                        fjname + (lang == Language.JAVA ? "$" : "__") + "IsNull",
                        isArray ? null : cgx.falseLiteral()
                );

            variable = cimpl.addVar(
                    Modifier.PUBLIC,// | Modifier.FINAL,
                    (isArray ? (JTypeImpl) () -> "System.Collections.Generic.IList<object>" :
                            (JTypeImpl) () -> "RTMath.Containers.BinaryArray"),
                    fjname,
                    isArray ? null : cgx.newExpr(((JTypeImpl) () -> "RTMath.Containers.BinaryArray"))
            );
        }

        if (variable != null)
            variable.addComment(df.getDescription());

        return variable;
    }

    private String toCSNamespace(String javaNamespace) {
        if (javaNamespace == null) return null;

        final String[] folders = javaNamespace.split("[.]");
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < folders.length; i++) {
            builder.append(toCSCodeNotation(folders[i]));
            if (i < folders.length - 1)
                builder.append('.');
        }

        return builder.length() > 0 ? builder.toString() : javaNamespace;
    }

    private String toCSCodeNotation(String javaName) {
        return javaName != null ? Character.toUpperCase(javaName.charAt(0)) + javaName.substring(1) : null;
    }

    private String toCSEnum(String javaName) {
        if (javaName == null) return null;

        final String[] subNames = javaName.split("[_]");
        final StringBuilder builder = new StringBuilder();
        for (String subName : subNames) {
            builder.append(toCSCodeNotation(subName.toLowerCase()));
        }

        return builder.length() > 0 ? builder.toString() : toCSCodeNotation(javaName.toLowerCase());
    }

    private ClassImpl   addAndProcess (ClassDescriptor cd) {
        return (process (addClass (cd)));
    }

    private ClassImpl   process(Bean bean) {
        switch (bean.state) {
            case PROCESSED:
                return (bean.cimpl);

            case WIP:
                throw new IllegalArgumentException (
                    "Circular nesting or inheritance involving " +
                    bean.pack + "." + bean.className
                );

            case UNPROCESSED:
                break;

            default:
                throw new RuntimeException (bean.state.name ());
        }

        bean.state = PState.WIP;

        ClassDescriptor cd = bean.cd;
        ClassImpl       cimpl = null;

        if (bean.cls == null) {
            if (cd instanceof RecordClassDescriptor)
                cimpl = processRecord (bean);
            else if (cd instanceof EnumClassDescriptor)
                cimpl = processJavaEnum(bean.pack, bean.className, (EnumClassDescriptor) cd);
            else
                throw new RuntimeException (cd.toString ());

            bean.cimpl = cimpl;
        }

        bean.state = PState.PROCESSED;

        return (cimpl);
    }

    private static final Bean []    BEAN_ARRAY_TAG = { };

    public final void           process () {
        for (Bean bean : beans ().toArray (BEAN_ARRAY_TAG))  // take snapshot
            process (bean);
    }

    public Collection <Bean>    beans () {
        return (beans.values ());
    }

    /**
     * @return not null value for Market and Security messages
     */
    public static String getName (final RecordClassDescriptor type) {
        if (type == null)
            return null;

        // TODO: @LEGACY
        final String javaClassName = type.getName ();
        if (javaClassName != null && javaClassName.endsWith(".MarketMessage"))
            return javaClassName;

        return getName (type.getParent ());
    }

    public ClassLoader          compile ()  {
        try {
            Map <String, String>        sources = new HashMap <> ();

            for (JavaBeanGenerator.Bean bean : beans ()) {
                if (bean.cls != null)
                    continue;

                String              source = bean.getSourceCode ();

                if (DUMP_SOURCES)
                    System.out.println (source);

                sources.put (bean.getNativeClassName (), source);
            }

            if (sources.isEmpty ())
                return (CL);

            final JavaCompilerHelper  jch = new JavaCompilerHelper (CL);

            for (Map.Entry <String, Class <?>> e : jch.compileClasses (sources).entrySet ())
                getByNativeClassName (e.getKey ()).cls = e.getValue ();

            return (jch.getClassLoader ());
        } catch (ClassNotFoundException x) {
            throw new RuntimeException ("Unexpected", x);
        } catch (CompilationExceptionWithDiagnostic x) {
            if (!DUMP_SOURCES)
                for (Bean bean : beans ())
                    System.out.println (bean.getSourceCode ());

            throw x;
        }
    }

    public TypeLoader           getTypeLoader () {
        return (
            new TypeLoader () {
                public Class <?> load (ClassDescriptor cd) throws ClassNotFoundException {
                    String          className = cd.getName ();
                    Bean            bean = beansBySchemaClassName.get (className);

                    if (bean == null)
                        throw new ClassNotFoundException ("No native class for: " + className);

                    if (bean.cls == null)
                        throw new ClassNotFoundException ("Bean has not been (successfully) compiled: " + className);

                    return (bean.cls);
                }
            }
        );
    }
}


