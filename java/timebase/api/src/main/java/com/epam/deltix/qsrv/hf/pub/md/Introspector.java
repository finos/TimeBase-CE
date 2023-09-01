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
package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.qsrv.hf.codec.ArrayTypeUtil;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.collections.CollectionUtil;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.containers.interfaces.BinaryArrayReadOnly;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Introspector used to analyze message structures. Also used by Super Form GUI.
 * By default search only:
 *  - public fields with and without @SchemaElement
 *  - public properties with @SchemaElement on 'getter' only
 */
public final class Introspector {

    private static final Logger LOG = Logger.getLogger(Introspector.class.getName());

    private ClassAnnotator annotator = ClassAnnotator.DEFAULT;
    private final Map<String, ClassDescriptor> mClassesByName = new TreeMap<String, ClassDescriptor>();
    private static RecordClassDescriptor[] classicMessagesDescriptors;

    /**
     * Exception that reports some kind of problem about message class or algorithm
     */
    public static class IntrospectionException extends Exception {
        public IntrospectionException (String msg) {
            super(msg);
        }

        public IntrospectionException (Exception x) {
            super(x);
        }
    }

    /**
     * By default introspect only:
     * - public field with/without @SchemaElement
     * - public properties ONLY with @SchemaElement on getter
     *
     * If "true" than introspect all public methods
     */
    private final boolean introspectAllMethods;

    /**
     * Enables special enum introspection with int values
     * extract from class if it is luminary-generated with method 'int getNumber()'.
     */
    private final boolean introspectEnumNew;

    // Use createMessageIntrospector() factory methods
    private Introspector () {
        this(false, true);
    }

    private Introspector (boolean introspectAllMethods) {
        this(introspectAllMethods, true);
    }

    private Introspector (boolean introspectAllMethods, boolean introspectEnumNew) {
        this.introspectAllMethods = introspectAllMethods;
        this.introspectEnumNew = introspectEnumNew;
    }

    /**
     * Creates Introspector with ability to check all public methods
     * @return Introspector instance
     */
    public static Introspector createCustomIntrospector () {
        return new Introspector(true);
    }
//
//    public static Introspector createMessageIntrospector (boolean introspectAllMethods) {
//        Introspector ix = new Introspector(introspectAllMethods);
//        ix.register(getStandardMarketMessageDescriptors());
//        return (ix);
//    }

    public static Introspector createEmptyMessageIntrospector () {
        return new Introspector();
    }

    /**
     * Creates Introspector instance that generates enum values according to 'ordinal' values
     */
    public static Introspector createOldIntrospector() {
        return new Introspector(false, false);
    }


//    public static Introspector createMessageIntrospector () {
//        Introspector ix = new Introspector();
//        ix.register(getStandardMarketMessageDescriptors());
//        return (ix);
//    }
//
//    public static RecordClassDescriptor[] getStandardMarketMessageDescriptors () {
//        if (classicMessagesDescriptors == null) {
//            final Integer staticCurrency = 840; //USD
//
//            RecordClassDescriptor abstractMarketMessage = StreamConfigurationHelper.mkMarketMessageDescriptor(staticCurrency, null, null);
//
//            classicMessagesDescriptors = new RecordClassDescriptor[] {
//                    StreamConfigurationHelper.mkTradeMessageDescriptor(abstractMarketMessage, null, staticCurrency, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO),
//                    StreamConfigurationHelper.mkBBOMessageDescriptor(abstractMarketMessage, true, null, staticCurrency, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO),
//                    StreamConfigurationHelper.mkBarMessageDescriptor(abstractMarketMessage, null, staticCurrency, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO),
//                    StreamConfigurationHelper.mkLevel2MessageDescriptor(abstractMarketMessage, null, staticCurrency, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO),
//                    StreamConfigurationHelper.mkL2SnapshotMessageDescriptor(abstractMarketMessage, null, staticCurrency, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO)
//            };
//        }
//        return classicMessagesDescriptors;
//    }

    public void register (RecordClassDescriptor[] types) {
        for (RecordClassDescriptor type : types) {
            register(type);
            type.visitDependencies(cd -> {
                register(cd);
                return true;
            });
        }
    }

    public EnumClassDescriptor introspectEnumClass (Class<?> cls)
            throws IntrospectionException {
        ClassDescriptor cmd = getClassDescriptor(cls);

        if (cmd != null && ! (cmd instanceof EnumClassDescriptor))
            throw new IntrospectionException(
                    "Class " + fullClassName(cls) + " is not an enumerated class."
            );

        EnumClassDescriptor md = (EnumClassDescriptor) cmd;

        if (md != null)
            return (md);

        LOG.log(Level.FINER, "[Introspector] create EnumClassDescriptor >> class: [{0}]", cls);

        if (introspectEnumNew) {
            md = introspectEnumNew(cls);
        } else {
            md = new EnumClassDescriptor(cls);
        }

        register(md);
        return (md);
    }

    private EnumClassDescriptor introspectEnumNew(Class<?> cls) throws IntrospectionException {
        String name = ClassAnnotator.DEFAULT.getName(cls);
        String title = ClassAnnotator.DEFAULT.getTitle(cls);

        int                     num;
        Object []               consts = cls.getEnumConstants ();
        num = consts.length;

        EnumValue[] values = new EnumValue [num];

        boolean bitmask = cls.isAnnotationPresent (Bitmask.class);

        Method method = null;
        try {
            method = cls.getMethod("getNumber");
        } catch (NoSuchMethodException | SecurityException ignored) {
        }

        if (method == null) {
            for (int ii = 0; ii < num; ii++) {
                Enum<?> eval = (Enum<?>) consts[ii];

                values[ii] = new EnumValue(eval.name(),bitmask ? (1 << ii) : ii);

            }
        } else {
            for (int i = 0; i < num; i++) {
                Enum<?> eval = (Enum<?>) consts[i];
                try {
                    values[i] = new EnumValue(eval.name(), (int) method.invoke(eval));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IntrospectionException(e);
                }
            }
        }

        return new EnumClassDescriptor(name, title, bitmask, values);
    }

    public void register (ClassDescriptor cd) {
        String name = cd.getName();
        ClassDescriptor prev = mClassesByName.put(name, cd);
        if (prev != null && ! prev.equals(cd)) {
            mClassesByName.put(name, prev);
            throw new IllegalArgumentException("Class Descriptors with different GUIDs should not use the same name: \"" + name + "\" and cannot appear in the same scope. Please rename one of them in TimeBase Administrator.");
        }
    }

    public ClassDescriptor getClassDescriptor (Class cls) {
        return mClassesByName.get(ClassDescriptor.getClassNameWithAssembly(cls));
    }

    public RecordClassDescriptor introspectRecordClass (Class<?> cls)
            throws IntrospectionException {
        return (introspectRecordClass("Explicit request", cls));
    }

    public RecordClassDescriptor introspectRecordClass (String whyLookedAt, Class<?> cls)
            throws IntrospectionException {
        return (introspectRecordClass(whyLookedAt, cls, null));
    }

    public RecordClassDescriptor introspectRecordClass (
            String whyLookedAt,
            Class<?> cls,
            Collection<DataField> additionalFields
    )
            throws IntrospectionException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[Introspector] introspectRecordClass >> whyLookedAt: [{0}], class: [{1}], additionalFields: {2}",
                    new Object[] {whyLookedAt, cls, CollectionUtil.toString(additionalFields, " ,")});
        }

        ClassDescriptor cmd = getClassDescriptor(cls);

        if (cmd != null && ! (cmd instanceof RecordClassDescriptor))
            throw new IntrospectionException(
                    "Class " + fullClassName(cls) + whyLookedAt + " is not a message class."
            );

        if (cls.equals(InstrumentMessage.class)) {
            return
                    new RecordClassDescriptor(
                            cls,
                            annotator,
                            null,
                            new DataField[0]
                    );
        }

        if (! InstrumentMessage.class.isAssignableFrom(cls))
            throw new IntrospectionException("Type of message " + whyLookedAt + " must be a subclass of " + InstrumentMessage.class.getName() + " instead of " + cls.getName());

        RecordClassDescriptor md = (RecordClassDescriptor) cmd;

        if (md != null)
            return (md);

        final Class<?> parentClass = cls.getSuperclass();
        List<DataField> fields = new ArrayList<>();

        final RecordClassDescriptor parent;
        if (parentClass == InstrumentMessage.class) {
            parent = null;
        } else {
            if (parentClass == null || parentClass.getName().equals("cli.System.Object") || parentClass.getName().equals("java.lang.Object")) {
                //parent = null;
                throw new IntrospectionException(
                        "Class " + fullClassName(cls) + whyLookedAt + " does not inherit InstrumentMessage type."
                );
            } else {
                parent = introspectRecordClass(" (parent of ", parentClass);
            }
        }

        md = new RecordClassDescriptor(cls, annotator, parent, RecordClassDescriptor.NO_FIELDS);
        register(md);

        for (Field f : cls.getDeclaredFields()) {

            if (!canIntrospect(f))
                continue;

//            if (Modifier.isStatic(f.getModifiers()) ||
//                    Modifier.isTransient(f.getModifiers()) ||
//                    f.isAnnotationPresent(SchemaIgnore.class) ||
//                    f.getDeclaringClass() != cls)
//                continue;

            if (f.isSynthetic())
                throw new IntrospectionException(whyLookedAt + " " + id(f) + " synthetic field is not supported");

            if (parent != null && parent.hasField(f.getName()))
                throw new IntrospectionException(whyLookedAt + " " + id(f) + " obscures parent");

            fields.add(introspectField(f));
        }

        for (Method m : cls.getDeclaredMethods()) {

            if (!canIntrospect(m))
                continue;

            if (!m.isAnnotationPresent(SchemaElement.class))
                continue;

            if (m.isSynthetic())
                throw new IntrospectionException(whyLookedAt + " " + id(m) + " synthetic methods is not supported");

            SchemaElement schemaElement = m.getAnnotation(SchemaElement.class);
            if (parent != null && parent.hasField(schemaElement.name()))
                throw new IntrospectionException(whyLookedAt + " " + id(m) + " obscures parent");

            final String fieldName = getName(m, schemaElement);

            checkDuplicateFieldName(fields, fieldName);

            fields.add(introspectMethod(m, fieldName));
        }

        if (introspectAllMethods) {
            for (Method m : cls.getDeclaredMethods()) {

                if (!canIntrospect(m))
                    continue;

                if (m.isAnnotationPresent(SchemaElement.class)) // all methods with SchemaElement introspected on previous step
                    continue;

                if (m.isSynthetic())
                    throw new IntrospectionException(whyLookedAt + " " + id(m) + " synthetic methods is not supported");

                final String fieldName = analyzeMethod(m);
                if (fieldName == null)
                    continue;

                if (parent != null && parent.hasField(fieldName))
                    throw new IntrospectionException(whyLookedAt + " " + id(m) + " obscures parent");

                checkDuplicateFieldName(fields, fieldName);

                fields.add(introspectMethod(m, fieldName));
            }
        }

        boolean abstr = annotator.isAbstract(cls);

        if (! abstr) {
            try {
                cls.getDeclaredConstructor();
            } catch (Exception x) {
                throw new IntrospectionException(
                        "Class " + fullClassName(cls) + whyLookedAt +
                                " must have a no-argument constructor"
                );
            }
        }

        if (additionalFields != null)
            fields.addAll(additionalFields);

        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "[Introspector] create RecordClassDescriptor >> class: [{0}], parent: [{1}], fields: {2}",
                    new Object[] {cls, parent, CollectionUtil.toString(fields, " ,")});
        }

        DataField[] fieldsArray = fields.toArray(new DataField[fields.size()]);
        Arrays.sort(fieldsArray, (Comparator.comparing(o -> o.getName().toLowerCase())));

        md.changeFields(fieldsArray);

        return (md);
    }

    private boolean     canIntrospect(Method m) {

        // skip bridge methods
        if (m.isBridge())
            return false;

        // skip static methods
        if (Modifier.isStatic(m.getModifiers()))
            return false;

        // skip "ignored" methods
        if (m.isAnnotationPresent(SchemaIgnore.class))
            return false;

        // only public methods supported
        return Modifier.isPublic(m.getModifiers());
    }

    private boolean     canIntrospect(Field field) {

        // skip static fields and transient
        if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))
            return false;

        // skip "ignored" methods
        if (field.isAnnotationPresent(SchemaIgnore.class))
            return false;

        // only public methods supported
        return Modifier.isPublic(field.getModifiers());
    }

    private void checkDuplicateFieldName (List<DataField> fields, String fieldName) throws IntrospectionException {
        for (DataField field : fields)
            if (Util.xequals(field.getName().toLowerCase(), fieldName.toLowerCase()))
                throw new IntrospectionException("Field \"" + fieldName + "\" can not be repeated in schema!");
    }


    public RecordClassDescriptor introspectMemberClass (
            String whyLookedAt,
            Class<?> cls
    )
            throws IntrospectionException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "[Introspector] introspectMemberClass >> whyLookedAt: [{0}], class: [{1}]",
                    new Object[] {whyLookedAt, cls});
        }

        ClassDescriptor cmd = getClassDescriptor(cls);
        RecordClassDescriptor md = (RecordClassDescriptor) cmd;

        if (md != null)
            return (md);

        final Class<?> parentClass = cls.getSuperclass();
        List<DataField> fields = new ArrayList<DataField>();

        final RecordClassDescriptor parent;

        if (parentClass == null || parentClass == Object.class || parentClass == InstrumentMessage.class) {
            parent = null;
        } else {
            parent = introspectMemberClass(" (parent of ", parentClass);
        }

        md = new RecordClassDescriptor(cls, annotator, parent, RecordClassDescriptor.NO_FIELDS);
        register(md);

        for (Field f : cls.getDeclaredFields()) {

            if (!canIntrospect(f))
                continue;

            if (parent != null && parent.hasField(f.getName()))
                throw new IntrospectionException(
                        id(f) + " obscures parent"
                );

            if (f.isSynthetic())
                throw new IntrospectionException(
                        whyLookedAt + " " + id(f) + " synthetic field is not supported"
                );

            fields.add(introspectField(f));
        }

        for (Method m : cls.getDeclaredMethods()) {
            if (!canIntrospect(m))
                continue;

            if (!m.isAnnotationPresent(SchemaElement.class))
                continue;

            SchemaElement schemaElement = m.getAnnotation(SchemaElement.class);
            if (parent != null && parent.hasField(schemaElement.name()))
                throw new IntrospectionException(
                        id(m) + " obscures parent"
                );

            if (m.isSynthetic())
                throw new IntrospectionException(
                        whyLookedAt + " " + id(m) + " synthetic field is not supported"
                );

            final String fieldName = getName(m, schemaElement);

            checkDuplicateFieldName(fields, fieldName);
            fields.add(introspectMethod(m, fieldName));
        }

        if (introspectAllMethods) {
            for (Method m : cls.getDeclaredMethods()) {

                if (!canIntrospect(m))
                    continue;

                // all methods with SchemaElement introspected on previous step
                if (m.isAnnotationPresent(SchemaElement.class))
                    continue;

                final String fieldName = analyzeMethod(m);
                if (fieldName == null)
                    continue;

                if (parent != null && parent.hasField(fieldName))
                    throw new IntrospectionException(
                            id(m) + " obscures parent"
                    );

                checkDuplicateFieldName(fields, fieldName);
                fields.add(introspectMethod(m, fieldName));
            }
        }

        if (!annotator.isAbstract(cls) && !cls.isEnum()) {
            try {
                cls.getDeclaredConstructor();
            } catch (Exception x) {
                throw new IntrospectionException(
                        "Class " + fullClassName(cls) + whyLookedAt +
                                " must have a no-argument constructor"
                );
            }
        }

        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "[Introspector] create ClassDescriptor >> class: [{0}], parent: [{1}], fields: {2}",
                    new Object[] {cls, parent, CollectionUtil.toString(fields, " ,")});
        }

        DataField[] fieldsArray = fields.toArray(new DataField[fields.size()]);
        Arrays.sort(fieldsArray, (Comparator.comparing(o -> o.getName().toLowerCase())));

        md.changeFields(fieldsArray);

        return (md);
    }

    /**
     * @return fieldName if @param method - getter and will be find setter, else return null.
     */
    private String analyzeMethod (Method method) {
        if (! method.getReturnType().equals(Void.TYPE) && method.getParameterCount() == 0)
            return extractFieldName(method);
        else {
            LOG.warning("Method \"" + method.getDeclaringClass().getName() + "#" + method.getName() + "\" cannot create a new schema element because it does not meet the getter method criteria: return VOID type and have 0 (zero) parameter count.\n");
            return null;
        }
    }

    private boolean isBooleanReturnType (Method method) {
        return ((method.getReturnType() == boolean.class) ||
                (method.getAnnotation(SchemaType.class) != null && method.getAnnotation(SchemaType.class).dataType() == SchemaDataType.BOOLEAN));
    }

    /**
     * @param method - getter method that using for creation new schema field
     * @return - field name in camel case notation. Or null if getter & setter doesn`t writing in Java Beans notation
     */
    private String extractFieldName (Method method) {
        return isBooleanReturnType(method) ?
                convertToCamelCase(getBooleanFieldName(method)) :
                convertToCamelCase(getFieldName(method));
    }

    private String convertToCamelCase (String string) {
        return string != null ? Character.toLowerCase(string.charAt(0)) + string.substring(1) : null;
    }

    private String getName (Method method, SchemaElement schemaElement) {
        return (Util.xequals(schemaElement.name(), "")) ?
                extractFieldName(method) :
                schemaElement.name();
    }

    private String getFieldName (Method method) {
        final String getterName = method.getName();

        if ((getterName.toLowerCase().startsWith("get"))) {
            final String expectedSetterName = ("set" + getterName.substring(3)).toLowerCase();
            for (Method m : method.getDeclaringClass().getMethods())
                if (Util.xequals(m.getName().toLowerCase(), expectedSetterName) && method.getDeclaringClass() == m.getDeclaringClass())
                    return getterName.substring(3);


            LOG.warning("Expected setter \"" + method.getDeclaringClass().getName() + "#" + expectedSetterName + "\" (for use with getter \"" + getterName + "()\") is not found.\n");
        } else
            LOG.warning("Method \"" + method.getDeclaringClass().getName() + "#" + getterName + "\" does not specify Java Bean notation for non-Boolean type. Expected name: get<Name>().\n");
        return null;
    }

    private String getBooleanFieldName (Method method) {
        final String getterName = method.getName();
        final String nameWithoutAnyPrefix = (getterName.toLowerCase().startsWith("get")) ?
                getterName.substring(3) :
                (getterName.toLowerCase().startsWith("is") ?
                        getterName.substring(2) :
                        getterName);

        final String expectedSetter = ("set" + getterName).toLowerCase();
        final String expectedSetterWithoutBooleanPrefix = ("set" + nameWithoutAnyPrefix).toLowerCase();

        for (Method m : method.getDeclaringClass().getMethods())
            if (Util.xequals(m.getName().toLowerCase(), expectedSetter) && m.getDeclaringClass() == method.getDeclaringClass())
                return getterName;
            else if (Util.xequals(m.getName().toLowerCase(), expectedSetterWithoutBooleanPrefix) && m.getDeclaringClass() == method.getDeclaringClass())
                return nameWithoutAnyPrefix;

        final StringBuilder builder = new StringBuilder();
        builder.append("Expected setter \"").append(method.getDeclaringClass().getName()).append('#').append(expectedSetter).append("\" ");
        if (! expectedSetter.equals(expectedSetterWithoutBooleanPrefix))
            builder.append("or \"").append(expectedSetterWithoutBooleanPrefix).append("\" ");
        builder.append("(for use with getter \"").append(method.getDeclaringClass().getName()).append('#').append(getterName).append("()\") ");
        builder.append("is not found.\n");
        LOG.warning(builder.toString());
        return null;
    }

    @SuppressWarnings ("unchecked")
    DataField introspectMethod (Method m, String name)
            throws IntrospectionException {

        if (name == null)
            throw new IntrospectionException("Invalid field name for method: " + m.getName());

        Class<?> cls = m.getReturnType();
        Class<?> genericCls = (m.getGenericReturnType() instanceof ParameterizedType) ?
                (Class<?>) ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments()[0] :
                null;
        DataType type;

        final SchemaType schemaType = m.getAnnotation(SchemaType.class);
        if (schemaType != null) {
            type = parseSchemaType(schemaType, name, cls, genericCls);
            return annotator.isStatic(m) ?
                    new StaticDataField(name, m, annotator, type) :
                    new NonStaticDataField(name, m, annotator, type);
        }

        final SchemaArrayType arrayType = m.getAnnotation(SchemaArrayType.class);
        if (arrayType != null) {
            type = parseSchemaArrayType(arrayType, name, cls, genericCls);
            return annotator.isStatic(m) ?
                    new StaticDataField(name, m, annotator, type) :
                    new NonStaticDataField(name, m, annotator, type);
        }

        type = getDataType(name, cls, genericCls, cls != boolean.class, null, null, null, true, null, null, null);
        if (type == null)
            throw new IntrospectionException(
                    "Type " + cls.getName() + " of " +
                            m.getDeclaringClass().getName() + "." + name + " is illegal"
            );


        return annotator.isStatic(m) ?
                new StaticDataField(name, m, annotator, type) :
                new NonStaticDataField(name, m, annotator, type);

    }

    @SuppressWarnings ("unchecked")
    DataField introspectField (Field f)
            throws IntrospectionException {
        String name = f.getName();
        Class<?> cls = f.getType();

        Class<?> genericCls = (f.getGenericType() instanceof ParameterizedType) ?
                (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0] :
                null;
        DataType type;


        final SchemaType schemaType = f.getAnnotation(SchemaType.class);
        if (schemaType != null) {
            type = parseSchemaType(schemaType, name, cls, genericCls);
            return annotator.isStatic(f) ?
                    new StaticDataField(f, annotator, type) :
                    new NonStaticDataField(f, annotator, type);
        }
        final SchemaArrayType arrayType = f.getAnnotation(SchemaArrayType.class);
        if (arrayType != null) {
            type = parseSchemaArrayType(arrayType, name, cls, genericCls);
            return annotator.isStatic(f) ?
                    new StaticDataField(f, annotator, type) :
                    new NonStaticDataField(f, annotator, type);
        }


        type = getDataType(name, cls, genericCls, cls != boolean.class, null, null, null, true, null, null, null);
        if (type == null)
            throw new IntrospectionException(
                    "Type " + cls.getName() + " of " +
                            f.getDeclaringClass().getName() + "." + name + " is illegal"
            );


        DataField df =
                annotator.isStatic(f) ?
                        new StaticDataField(f, annotator, type) :
                        new NonStaticDataField(f, annotator, type);

        return (df);
    }

    public DataType parseSchemaType (SchemaType schemaType, String fieldName, Class<?> cls, Class<?> genericCls) throws Introspector.IntrospectionException {
        SchemaDataType type = schemaType.dataType();
        if (type.equals(SchemaDataType.DEFAULT))
            return getDataType(fieldName, cls, genericCls, schemaType.isNullable(), schemaType.encoding(), schemaType.minimum(), schemaType.maximum(), true, null, null, null);
        else
            return getDataType(
                    schemaType.dataType(),
                    schemaType.encoding(),
                    schemaType.nestedTypes(),
                    schemaType.isNullable(),
                    schemaType.minimum(),
                    schemaType.maximum(),
                    cls,
                    fieldName
            );
    }

//    public DataType parseSchemaType (SchemaStaticType schemaType, String fieldName, Class<?> cls, Class<?> genericCls) throws Introspector.IntrospectionException {
//        SchemaDataType type = schemaType.dataType();
//        if (type.equals(SchemaDataType.DEFAULT))
//            return getDataType(fieldName, cls, genericCls, schemaType.isNullable(), schemaType.encoding(), schemaType.minimum(), schemaType.maximum(), true, null, null, null);
//        else
//            return getDataType(
//                    schemaType.dataType(),
//                    schemaType.encoding(),
//                    null,
//                    schemaType.isNullable(),
//                    schemaType.minimum(),
//                    schemaType.maximum(),
//                    cls,
//                    fieldName
//            );
//    }

    private DataType getDataType (SchemaDataType dataType,
                                  String encoding,
                                  Class<?>[] nestedTypes,
                                  boolean isNullable,
                                  String min,
                                  String max,
                                  Class<?> fieldType,
                                  String fieldName) throws Introspector.IntrospectionException {
        switch (dataType) {
            case CHAR:
                return new CharDataType(isNullable);

            case VARCHAR:
                return new VarcharDataType(encoding.equals("") ? VarcharDataType.ENCODING_INLINE_VARSIZE : encoding, isNullable, true);

            case BINARY:
                return new BinaryDataType(isNullable, 0);

            case BOOLEAN:
                return new BooleanDataType(isNullable);

            case INTEGER:
                if (min != null && max != null && ! min.equals("") && ! max.equals(""))
                    return new IntegerDataType(encoding, isNullable, Long.parseLong(min), Long.parseLong(max));
                else
                    return new IntegerDataType(encoding, isNullable);

            case FLOAT:
                if (min != null && max != null && ! min.equals("") && ! max.equals(""))
                    return new FloatDataType(encoding, isNullable, Double.parseDouble(min), Double.parseDouble(max));
                else
                    return new FloatDataType(encoding, isNullable);

            case TIMESTAMP:
                return new DateTimeDataType(isNullable);

            case TIME_OF_DAY:
                return new TimeOfDayDataType(isNullable);

            case OBJECT:
                if (Object.class.isAssignableFrom(fieldType))
                    return getClassDataType(fieldName, nestedTypes, isNullable);

            case ENUM:
                if (MdUtil.isIntegerType(fieldType) || fieldType.isEnum() || fieldType == CharSequence.class)
                    return (getEnumDataType(nestedTypes, fieldType, isNullable));
                else
                    throw new IllegalArgumentException("invalid binding for enum " + fieldType.getName());

            default:
                throw new UnsupportedOperationException("Unsupported type " + dataType);
        }
    }

    private DataType getElementDataType(String memberName, Class<?> declaredType, Class<?>[] nestedTypes, boolean isNullable) throws IntrospectionException {
        if (declaredType == CharSequence.class) {
            return new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, isNullable, true);
        }
        return declaredType.isEnum() ? getEnumDataType(nestedTypes, declaredType, isNullable) :
                getClassDataType(memberName, nestedTypes, isNullable);
    }

    private DataType getEnumDataType (Class<?>[] nestedTypes, Class<?> fieldType, boolean isNullable) throws Introspector.IntrospectionException {
        final Class<?> ecls = (nestedTypes.length > 0) ? nestedTypes[0] : fieldType;

        EnumClassDescriptor ecd = (EnumClassDescriptor) getClassDescriptor(ecls);
        //
        // If not known, try and load by introspected class loader.
        //
        if (ecd == null)
            ecd = introspectEnumClass(ecls);

        return (new EnumDataType(isNullable, ecd));
    }

    private DataType getClassDataType (String fieldName, Class<?>[] classes, boolean isNullable) throws Introspector.IntrospectionException {
        final ArrayList<RecordClassDescriptor> rcds = new ArrayList<>();

        for (Class<?> cls0 : classes)
            rcds.add(introspectMemberClass(" class field " + fieldName, cls0));

        if (rcds.isEmpty())
            throw new IllegalArgumentException("object field " + fieldName + " has no message types defined");
        else
            return new ClassDataType(isNullable, rcds.toArray(new RecordClassDescriptor[rcds.size()]));
    }

    public DataType parseSchemaArrayType (SchemaArrayType arrayType, String fieldName, Class<?> cls, Class<?> genericCls) throws Introspector.IntrospectionException {
        if (arrayType.elementTypes().length > 0) {
            final Class<?>[] classes = arrayType.elementTypes();
            DataType type = getElementDataType(fieldName, genericCls, classes, arrayType.isElementNullable());

            return new ArrayDataType(arrayType.isNullable(), type);
        } else if (! arrayType.elementDataType().equals(SchemaDataType.DEFAULT)) {
            return new ArrayDataType(
                    arrayType.isNullable(),
                    getDataType(
                            arrayType.elementDataType(),
                            arrayType.elementEncoding(),
                            arrayType.elementTypes(),
                            arrayType.isElementNullable(),
                            arrayType.elementMinimum(),
                            arrayType.elementMaximum(),
                            cls,
                            fieldName
                    )
            );
        } else {
            return getDataType(fieldName,
                    cls,
                    genericCls,
                    arrayType.isNullable(),
                    null,
                    null,
                    null,
                    arrayType.isElementNullable(),
                    arrayType.elementEncoding(),
                    arrayType.elementMinimum(),
                    arrayType.elementMaximum());
        }
    }


    public DataType getDataType (String fieldName,
                                 Class<?> cls,
                                 Class<?> genericCls,
                                 boolean nullable,
                                 String encoding,
                                 String min,
                                 String max,
                                 boolean elementNullable,
                                 String elementEncoding,
                                 String elementMin,
                                 String elementMax) throws IntrospectionException {

        if (cls == boolean.class)
            return new BooleanDataType(nullable);
        else if (cls == byte.class) {
            if (StringUtils.isEmpty(encoding))
                encoding = IntegerDataType.ENCODING_INT8;
        } else if (cls == char.class)
            return new CharDataType(nullable);
        else if (cls == short.class) {
            if (encoding == null || encoding.equals(""))
                encoding = IntegerDataType.ENCODING_INT16;
        } else if (cls == int.class) {
            if (encoding == null || encoding.equals(""))
                encoding = IntegerDataType.ENCODING_INT32;
        } else if (cls == long.class) {
            if (encoding == null || encoding.equals(""))
                encoding = IntegerDataType.ENCODING_INT64;
        } else if (cls == Decimal64.class) {
            if (encoding == null || encoding.equals(""))
                encoding = FloatDataType.ENCODING_DECIMAL64;
        }
        else if (cls == float.class) {
            if (encoding == null || encoding.equals(""))
                encoding = FloatDataType.ENCODING_FIXED_FLOAT;
        } else if (cls == double.class) {
            if (encoding == null || encoding.equals(""))
                encoding = FloatDataType.ENCODING_FIXED_DOUBLE;
        } else if (cls == String.class || cls == CharSequence.class || cls == StringBuilder.class) {
            if (encoding == null || encoding.equals(""))
                encoding = VarcharDataType.ENCODING_INLINE_VARSIZE;
            return new VarcharDataType(encoding, nullable, false);
        } else if (cls.isEnum() || cls.getSuperclass() != null && cls.getSuperclass().getName().equals("cli.System.Enum"))
            return new EnumDataType(nullable, introspectEnumClass(cls));
//        else if (cls.getName().equals("cli.System.DateTime"))
//            return new DateTimeDataType(nullable);
        else if (cls == ByteArrayList.class)
            return new BinaryDataType(nullable, 0);
        else if (cls.isAssignableFrom(BinaryArrayReadOnly.class))
            return new BinaryDataType(nullable, 0);
            // ARRAY fields
        else if (ArrayTypeUtil.isSupported(cls)) {
            if (cls == ObjectArrayList.class) {
                if (genericCls.isInterface() && genericCls != CharSequence.class)
                    throw new IntrospectionException("Generic interfaces does not support in array field without @SchemaArrayType annotation. Field: \"" + fieldName + "\".");

                DataType elementType = getElementDataType(fieldName, genericCls, new Class[] {genericCls}, elementNullable);
                return new ArrayDataType(nullable, elementType);
            } else {
                final Class<?> nestedClass = ArrayTypeUtil.getUnderline(cls);
                final DataType underlineDataType = getDataType(fieldName, nestedClass, null, elementNullable && nestedClass != boolean.class, elementEncoding, elementMin, elementMax, true, null, null, null);
                return (underlineDataType != null) ? new ArrayDataType(nullable, underlineDataType) : null;
            }
        } else if (Object.class.isAssignableFrom(cls))
            return getClassDataType(fieldName, cls, nullable);
        else
            return null;

        if (cls == float.class || cls == double.class || cls == Decimal64.class) {
            if (min != null && max != null && ! min.equals("") && ! max.equals(""))
                return new FloatDataType(encoding, nullable, Double.parseDouble(min), Double.parseDouble(max));
            else
                return new FloatDataType(encoding, nullable);
        } else {
            if (min != null && max != null && ! min.equals("") && ! max.equals(""))
                return new IntegerDataType(encoding, nullable, Integer.parseInt(min), Integer.parseInt(max));
            else
                return new IntegerDataType(encoding, nullable);
        }
    }

    public DataType getClassDataType (String fieldName, Class<?> cls, boolean isNullable) throws IntrospectionException {
        final ArrayList<RecordClassDescriptor> rcds = new ArrayList<>();

        if (! cls.isInterface() && (cls.getModifiers() & Modifier.ABSTRACT) == 0)
            rcds.add(introspectMemberClass(" class field " + fieldName, cls));

        if (rcds.isEmpty())
            throw new IllegalArgumentException("object field " + fieldName + " has no message types defined");
        else
            return new ClassDataType(isNullable, rcds.toArray(new RecordClassDescriptor[rcds.size()]));
    }


    public ClassDescriptor[] getAllClasses () {
        return (mClassesByName.values().toArray(new ClassDescriptor[mClassesByName.size()]));
    }

    /**
     * Use {@link Introspector#getClassDescriptor(Class)}
     */
    @Deprecated
    public ClassDescriptor getClassDescriptor (String name) {
        return mClassesByName.get(name);
    }


    public RecordClassDescriptor[] getRecordClasses () {
        List<RecordClassDescriptor> rcds =
                new ArrayList<RecordClassDescriptor>();

        for (ClassDescriptor cd : mClassesByName.values()) {
            if (cd instanceof RecordClassDescriptor) {
                RecordClassDescriptor rcd = (RecordClassDescriptor) cd;

                if (! rcd.isAbstract())
                    rcds.add(rcd);
            }
        }

        return (rcds.toArray(new RecordClassDescriptor[rcds.size()]));
    }

    /**
     * Gets record classes that represent {@code Class} object
     * is either the same as, or is a superclass,
     * the class represented by the specified {@code Class} parameter.
     * It returns {@code true} if so; otherwise it returns {@code false}.
     */

    public RecordClassDescriptor[] getRecordClasses (Class<?> from) {
        List<RecordClassDescriptor> rcds =
                new ArrayList<RecordClassDescriptor>();

        for (ClassDescriptor cd : mClassesByName.values()) {
            if (cd instanceof RecordClassDescriptor) {
                RecordClassDescriptor rcd = (RecordClassDescriptor) cd;

                if (! rcd.isAbstract() && isAssignableFrom(rcd, from))
                    rcds.add(rcd);
            }
        }

        return (rcds.toArray(new RecordClassDescriptor[rcds.size()]));
    }

    private boolean isAssignableFrom (RecordClassDescriptor rcd, Class<?> clazz) {
        if (rcd != null && rcd.getName().equals(clazz.getName()))
            return true;

        return rcd != null && isAssignableFrom(rcd.getParent(), clazz);
    }

    public static ClassDescriptor introspectSingleClass (Class<?> cls) throws IntrospectionException {
        try {
            return Introspector.createEmptyMessageIntrospector().introspectRecordClass("introspectSingle", cls);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private static String id (Member m) {
        return (m.getDeclaringClass().getName() + "." + m.getName());
    }

    private static String fullClassName (Class<?> cls) {
        return ClassDescriptor.getClassNameWithAssembly(cls);
    }

    @SuppressWarnings ("unused")
    public ClassAnnotator getClassAnnotator () {
        return annotator;
    }

    public void setClassAnnotator (ClassAnnotator annotator) {
        this.annotator = annotator;
    }


}