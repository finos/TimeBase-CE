package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.codec.BindValidator;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.timebase.messages.SchemaIgnore;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.lang.Util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *
 */
public class FieldLayout <T extends DataField> implements DataFieldInfo {
    private static final Log LOG = LogFactory.getLog(TypeLoaderImpl.class);

    private final RecordClassDescriptor         owner;
    private final T                             field;
    private final Class<?>                      nativeType;

    private Field                               jfield;
    private Method                              getter;
    private Method                              setter;
    private Method                              haser;
    private Method                              nullifier;
    private Class<?>                            genericClass;
    private boolean                             hasAccessMethods;
    private boolean                             hasSmartProperties;

    protected Class<?>                          fieldType;

    private boolean wasSearchingPublicProperties = false;

    public FieldLayout (RecordClassDescriptor owner, T field) {
        this.owner = owner;
        this.field = field;
        this.nativeType = RecordLayout.getNativeType(field.getType());
        this.hasAccessMethods = false;
    }

    @Override
    public String toString () {
        return (owner.getName() + "." + field.getName());
    }

    public T getField () {
        return field;
    }

    public Class<?> getNativeType () {
        return nativeType;
    }

    public RecordClassDescriptor getOwner () {
        return owner;
    }

    public String getName () {
        return field.getName();
    }

    public DataType getType () {
        return field.getType();
    }

    public String getTitle () {
        return field.getTitle();
    }

    final void bind (TypeLoader loader)
            throws NoSuchFieldException, ClassNotFoundException {
        bind(RecordLayout.getClassFor(loader, owner));
    }

    private boolean searchPublicField (Class<?> cls) throws NoSuchFieldException {
        final String name = field.getName();

        for (Field field : cls.getFields()) {   //looking for override public field
            SchemaElement schemaElement = field.getAnnotation(SchemaElement.class);
            if (schemaElement != null && Util.xequals(schemaElement.name().toLowerCase(), name.toLowerCase()))
                jfield = field;
        }

        if (jfield == null)
            for (Field field : cls.getFields()) {   //looking for no-override public field
                if (Util.xequals(name.toLowerCase(), field.getName().toLowerCase()))
                    jfield = field;
            }

        if (jfield != null) {
            fieldType = jfield.getType();

            if (isBoxed(fieldType))
                throw new UnsupportedOperationException("Boxed types are not supported due to performance degradation: " + jfield.getType().getName());

            if (jfield.getAnnotation(SchemaIgnore.class) != null)
                throw new NoSuchFieldException();

            try {
                // validate the bound type against DataType
                if (field instanceof NonStaticDataField)
                    BindValidator.validateType(field.getType(), jfield);
                    // validate static value against bound type
                else if (field instanceof StaticDataField)
                    BindValidator.validateTypeStatic(field.getType(), jfield, ((StaticDataField) field).getStaticValue());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage(), new RuntimeException(jfield.toString()));
            }

            if (jfield.getType().equals(ObjectArrayList.class)) {
                Type fieldGenericType = jfield.getGenericType();
                if (fieldGenericType != ObjectArrayList.class) {
                    ParameterizedType genericType = (ParameterizedType) jfield.getGenericType();
                    genericClass = (Class<?>) genericType.getActualTypeArguments()[0];
                }
            }
            return true;
        }

        return false;
    }


    private void lookingForProperties (Method getter, boolean isBooleanField) {
        final String getterName = getter.getName();
        String fieldName = (getterName.toLowerCase().startsWith("get")) ?
                getterName.substring(3) :
                getterName;

        Class<?> cls = getter.getDeclaringClass();

        if (isBooleanField) {
            String fieldNameWithoutIsPrefix = fieldName.toLowerCase().startsWith("is") ?
                    fieldName.substring(2) :
                    fieldName;

            for (Method m : cls.getMethods()) {
                if (Util.xequals(m.getName().toLowerCase(), ("set" + fieldName).toLowerCase())) {
                    if (isValidGetterAndSetter(getter, m, fieldName, true)) {
                        setGetter(getter);
                        setSetter(m);
                        break;
                    }
                } else if (Util.xequals(m.getName().toLowerCase(), ("set" + fieldNameWithoutIsPrefix).toLowerCase())) {
                    if (isValidGetterAndSetter(getter, m, fieldName, true)) {
                        fieldName = fieldNameWithoutIsPrefix;
                        setGetter(getter);
                        setSetter(m);
                        break;
                    }
                }
            }
        } else
            for (Method m : cls.getMethods()) {
                if (Util.xequals(m.getName().toLowerCase(), ("set" + fieldName).toLowerCase())) {
                    if (isValidGetterAndSetter(getter, m, fieldName, false)) {
                        setGetter(getter);
                        setSetter(m);
                        break;
                    }
                }
            }

        if (hasGetter() && hasSetter())
            for (Method method : cls.getMethods()) {
                if (hasNullifier() && hasHaser())
                    break;
                if (! hasNullifier())
                    validateNullifier(method, fieldName);
                if (! hasHaser())
                    validateHaser(method, fieldName);
            }
    }

    private boolean isValidGetterAndSetter (Method getter, Method setter, String fieldName, boolean isBooleanField) {
        return (isValidSetterType(setter, fieldName, isBooleanField) && isValidGetterType(getter, fieldName, isBooleanField));
    }

    private boolean searchPublicProperties (Class<?> cls) throws NoSuchFieldException {
        this.wasSearchingPublicProperties = true;
        final String fieldName = field.getName();
        final boolean isBooleanField = field.getType() instanceof BooleanDataType;   //case for "is" getter for boolean field

        // looking for properties with SchemaElement.name
        for (Method method : cls.getMethods()) {
            if (method.isBridge()) continue;

            if (hasGetter() && hasSetter())
                break;

            SchemaElement schemaElement = method.getAnnotation(SchemaElement.class);
            if (schemaElement != null && Util.xequals(schemaElement.name().toLowerCase(), fieldName.toLowerCase())) {
                lookingForProperties(method, isBooleanField);
            }
        }

        if (! hasGetter() && ! hasSetter()) {
            // looking for properties with empty SchemaElement
            for (Method method : cls.getMethods()) {
                if (hasGetter() && hasSetter())
                    break;

                if (method.isBridge()) continue;

                SchemaElement schemaElement = method.getAnnotation(SchemaElement.class);
                if (schemaElement != null && isValidGetterJavaBeanNotation(method, fieldName, isBooleanField)) {
                    lookingForProperties(method, isBooleanField);
                }
            }
        }

        if (! hasGetter() && ! hasSetter()) {
            // looking for properties with JavaBean Notation
            for (Method method : cls.getMethods()) {
                if (hasGetter() && hasSetter() && hasNullifier() && hasHaser())
                    break;

                if (method.isBridge()) continue;

                if (! hasGetter() && isValidGetterJavaBeanNotation(method, fieldName, isBooleanField) && isValidGetterType(method, fieldName, isBooleanField))
                    setGetter(method);

                if (! hasSetter() && isValidSetterJavaBeanNotation(method, fieldName) && isValidSetterType(method, fieldName, isBooleanField))
                    setSetter(method);

                if (! hasNullifier())
                    validateNullifier(method, fieldName);

                if (! hasHaser())
                    validateHaser(method, fieldName);
            }
        }

        if (hasGetter() && hasSetter()) {
            if (isBoxed(this.fieldType))
                throw new UnsupportedOperationException("Boxed types are not supported due to performance reason: " + jfield.getType().getName());

            if (getter.getAnnotation(SchemaIgnore.class) != null)
                throw new NoSuchFieldException();

            hasAccessMethods = true;

            if (hasNullifier() && hasHaser())
                hasSmartProperties = true;
            else
                logInfo(fieldName, cls);
            return true;
        } else {
            logWarning(fieldName, cls, isBooleanField);
            return false;
        }
    }


    private boolean isValidGetterJavaBeanNotation (Method method, String fieldName, boolean isBooleanDataType) {
        final String expectedGetterName = isBooleanDataType ? fieldName.toLowerCase() : ("get" + fieldName).toLowerCase();

        return isBooleanDataType ?
                (Util.xequals(expectedGetterName, method.getName().toLowerCase()) ||
                        Util.xequals("is" + expectedGetterName, method.getName().toLowerCase()) ||
                        Util.xequals("get" + expectedGetterName, method.getName().toLowerCase())) :
                (Util.xequals(expectedGetterName, method.getName().toLowerCase()));
    }

    private boolean isValidSetterJavaBeanNotation (Method method, String fieldName) {
        return Util.xequals(("set" + fieldName).toLowerCase(), method.getName().toLowerCase());
    }

    private boolean isValidGetterType (Method method, String fieldName, boolean isBooleanDataType) {
        if (method.getParameterCount() != 0) {
            LOG.log(CodecFactory.VALIDATION_LEVEL,"Method \"%s\" must have 0 parameters count for using as getter for field \"%s\".").with(getMethodName(method)).with(fieldName);
            return false;
        }

        if (isBooleanDataType) {
            if (method.getReturnType() == boolean.class || method.getReturnType() == byte.class) {
                return true;
            } else
                LOG.log(CodecFactory.VALIDATION_LEVEL, "Method \"%s\" must return boolean type for using as getter for field \"%s\".").with(getMethodName(method)).with(fieldName);
        } else {
            try {
                if (field instanceof NonStaticDataField) {
                    BindValidator.validateGenericType(field.getType(), method.getReturnType(), method.getGenericReturnType());
                } else if (field instanceof StaticDataField) {
                    BindValidator.validateTypeStatic(field.getType(), method.getReturnType(), null, method, ((StaticDataField) field).getStaticValue());
                }
                return true;
            } catch (IllegalArgumentException e) {
                LOG.log(CodecFactory.VALIDATION_LEVEL, "Method \"%s\" must return %s type for using as getter for field \"%s\".").with(getMethodName(method)).with(field.getType().getBaseName()).with(fieldName);
            }
        }
        return false;
    }

    private void setGetter (Method method) {
        getter = method;
        fieldType = method.getReturnType();
        Type fieldGenericType = method.getGenericReturnType();
        if (fieldGenericType != ObjectArrayList.class && fieldGenericType instanceof ParameterizedType) {
            ParameterizedType genericType = (ParameterizedType) method.getGenericReturnType();
            genericClass = (Class<?>) genericType.getActualTypeArguments()[0];
        }
    }

    private void setSetter (Method method) {
        this.setter = method;
    }

    private boolean isValidSetterType (Method method, String fieldName, boolean isBooleanDataType) {
        if (isBooleanDataType) {
            if (method.getParameterCount() == 1 && (method.getParameterTypes()[0] == boolean.class || method.getParameterTypes()[0] == byte.class))
                return true;
            else
                LOG.log(CodecFactory.VALIDATION_LEVEL, "Method \"%s\" must have 1 parameter of boolean or byte type for using as setter for field \"%s\".").with(getMethodName(method)).with(fieldName);

        } else {
            if (method.getParameterCount() == 1) {
                try {
                    if (field instanceof NonStaticDataField) {
                        BindValidator.validateGenericType(field.getType(), method.getParameterTypes()[0], method.getGenericParameterTypes()[0]);
                    } else if (field instanceof StaticDataField) {
                        BindValidator.validateTypeStatic(field.getType(), method.getParameterTypes()[0], null, method, ((StaticDataField) field).getStaticValue());
                    }
                    return true;
                } catch (IllegalArgumentException e) {
                    LOG.log(CodecFactory.VALIDATION_LEVEL, "Method \"%s\" must have parameter of %s type for using as setter for field \"%s\".").with(getMethodName(method)).with(field.getType().getBaseName()).with(fieldName);
                }
            } else
                LOG.log(CodecFactory.VALIDATION_LEVEL, "Method \"%s\" must have 1 parameter for using as setter for field \"%s\".").with(getMethodName(method)).with(fieldName);
        }
        return false;
    }

    private void validateHaser (Method method, String fieldName) {
        final String expectedHaserName = ("has" + fieldName).toLowerCase();
        if (Util.xequals(expectedHaserName, method.getName().toLowerCase())) {
            if (method.getReturnType() == boolean.class) {
                if (method.getParameterCount() == 0) {
                    haser = method;
                } else {
                    LOG.log(CodecFactory.VALIDATION_LEVEL, "Method \"%s\" must have 0 parameters count for using as has-er for field \"%s\".").with(getMethodName(method)).with(fieldName);
                }
            } else {
                LOG.log(CodecFactory.VALIDATION_LEVEL, "Method \"%s\" must return boolean type for using as has-er for field \"%s\".").with(getMethodName(method)).with(fieldName);
            }
        }
    }


    private void validateNullifier (Method method, String fieldName) {
        final String expectedNullifierName = ("nullify" + fieldName).toLowerCase();
        if (Util.xequals(expectedNullifierName, method.getName().toLowerCase())) {
            if (method.getParameterCount() == 0) {
                nullifier = method;
            } else {
                LOG.log(CodecFactory.VALIDATION_LEVEL, "Method \"%s\" must have 0 parameters count for using as nullifier for field \"%s\".").with(getMethodName(method)).with(fieldName);
            }
        }
    }


    public final void bind (Class<?> cls) {
        if (! field.getName().startsWith("dummy")) {     //requirements to migrate from 4.2 to 4.3
            final String name = field.getName();
            try {
                if (! searchPublicField(cls))
                    if (! wasSearchingPublicProperties)
                        searchPublicProperties(cls);

                if (jfield == null && ! hasAccessMethods)
                    throw new NoSuchFieldException("No such field: " + name);

            } catch (NoSuchFieldException e) {
                if (! lookup(cls)) {
                    if (cls.getSuperclass() != null)
                        bind(cls.getSuperclass());
                    else
                        jfield = null;
                }
            }
        }
    }


    public final boolean isBound () {
        return (jfield != null || hasAccessMethods);
    }

    public final Field getJavaField () {
        return (jfield);
    }

    public Class<?> getGenericClass () {
        return genericClass;
    }

    public final Class<?> getFieldType () {
        return (jfield != null) ? jfield.getType() : fieldType;
    }

    public final String getFieldName () {
        return (jfield != null) ? jfield.getName() : field.getName();
    }

    public final String getDescription () {
        return (jfield != null) ? jfield.toGenericString() : ((field.getDescription() == null && hasAccessMethods ) ? getter.toGenericString() : field.getDescription());
    }

    protected final void set (Object value, Object msgObject)
            throws IllegalArgumentException, IllegalAccessException {
        jfield.set(msgObject, value);
    }

    private boolean lookup (Class<?> cls) {
        final String name = field.getName();

        final Field[] fields = cls.getFields();
        for (Field f : fields) {
            final SchemaElement m = f.getAnnotation(SchemaElement.class);
            if (m != null && name.equals(m.name())) {
                jfield = f;
                return true;
            }
        }

        return (cls.getSuperclass() != null) && lookup(cls.getSuperclass());
    }

    private final static Package LANG_PACKAGE = Package.getPackage("java.lang");
    private final static Class<?>[] BOXED_CLASSES = {Boolean.class, Character.class,
            Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class};

    private static boolean isBoxed (Class<?> clazz) {
        return !clazz.isPrimitive() &&
                clazz.getPackage() == LANG_PACKAGE &&
                Util.indexOf(BOXED_CLASSES, clazz) != - 1;
    }


    public boolean hasAccessMethods () {
        return hasAccessMethods;
    }

    public boolean hasSmartProperties () {
        return hasSmartProperties;
    }

    public Method getGetter () {
        return getter;
    }

    public boolean hasGetter () {
        return this.getter != null;
    }

    public Class<?> getGetterReturnType () {
        return this.getter.getReturnType();
    }

    public Class<?> getSetterType () {
        return this.setter.getParameterTypes()[0];
    }

    public Method getSetter () {
        return setter;
    }

    public boolean hasSetter () {
        return this.setter != null;
    }

    public Method getHaser () {
        return haser;
    }

    public boolean hasHaser () {
        return this.haser != null;
    }

    public Method getNullifier () {
        return nullifier;
    }

    public boolean hasNullifier () {
        return this.nullifier != null;
    }

    private void logWarning (String fieldName, Class<?> cls, boolean isBooleanField) {
        final String fieldNameInMethod = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        final StringBuilder builder = new StringBuilder();
        builder.append("Field \"");
        builder.append(fieldName).append("\"");
        builder.append(" in class ").append(cls.getName());
        builder.append(" will be skipped. Reason is: ");
        if (! hasGetter()) {
            if (isBooleanField) {
                builder.append("Expected getters (\"");
                builder.append(fieldName);
                builder.append("\" or \"is");
                builder.append(fieldNameInMethod);
                builder.append("\" or \"get");
                builder.append(fieldNameInMethod);
                builder.append("\") is not found. ");
            } else {
                builder.append("Expected getter (\"get");
                builder.append(fieldNameInMethod);
                builder.append("\") is not found. ");
            }
        }
        if (! hasSetter()) {
            builder.append("Expected setter (\"set");
            builder.append(fieldNameInMethod);
            builder.append("\") is not found. ");
        }

        if (! hasNullifier()) {
            builder.append("Expected nullifier (\"nullify");
            builder.append(fieldNameInMethod);
            builder.append("\") is not found. ");
        }

        if (! hasHaser()) {
            builder.append("Expected haser (\"has");
            builder.append(fieldNameInMethod);
            builder.append("\") is not found. ");
        }

        LOG.log(CodecFactory.VALIDATION_LEVEL, builder.toString());
    }

    private void logInfo (String fieldName, Class<?> cls) {
        final String fieldNameInMethod = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        final StringBuilder builder = new StringBuilder();

        builder.append("For field \"").append(fieldName).append("\" ");
        builder.append(" in class ").append(cls.getName());
        builder.append(" will be use only getter and setter methods. Reason is: ");
        if (! hasNullifier()) {
            builder.append("Expected nullifier (\"nullify");
            builder.append(fieldNameInMethod);
            builder.append("\") is not found. ");
        }

        if (! hasHaser()) {
            builder.append("Expected haser (\"has");
            builder.append(fieldNameInMethod);
            builder.append("\") is not found. ");
        }

        LOG.log(CodecFactory.VALIDATION_LEVEL, builder.toString());
    }

    private String getMethodName (Method method) {
        return method.getDeclaringClass().getName() + '#' + method.getName();
    }
}