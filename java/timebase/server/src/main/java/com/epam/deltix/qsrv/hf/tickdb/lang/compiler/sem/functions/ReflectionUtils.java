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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions;

import com.epam.deltix.computations.api.annotations.Arg;
import com.epam.deltix.computations.api.annotations.BuiltInNanoTime;
import com.epam.deltix.computations.api.annotations.BuiltInStartNanoTime;
import com.epam.deltix.computations.api.annotations.BuiltInStartTimestampMs;
import com.epam.deltix.computations.api.annotations.BuiltInTimestampMs;
import com.epam.deltix.computations.api.annotations.Compute;
import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.Generic;
import com.epam.deltix.computations.api.annotations.GenericParameter;
import com.epam.deltix.computations.api.annotations.Init;
import com.epam.deltix.computations.api.annotations.Pool;
import com.epam.deltix.computations.api.annotations.Reset;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.computations.api.annotations.Type;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.functions.DB;
import com.epam.deltix.util.annotations.Bool;
import com.epam.deltix.util.annotations.TimeOfDay;
import com.epam.deltix.util.annotations.TimestampMs;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.collections.generated.CharacterArrayList;
import com.epam.deltix.util.collections.generated.DoubleArrayList;
import com.epam.deltix.util.collections.generated.FloatArrayList;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ShortArrayList;
import com.epam.deltix.util.lang.StringUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.util.ReflectionUtils.getUniqueDeclaredMethods;

public class ReflectionUtils {

    private static final Log LOG = LogFactory.getLog(ReflectionUtils.class);

    private static final String GENERIC_OBJECT_PATTERN = "%s(?<nullable>\\?)?";
    private static final String GENERIC_ARRAY_PATTERN = "ARRAY\\(T(?<nullable>\\?)?\\)(?<arrayNullable>\\?)?";

    private static final Pattern OBJECT_PATTERN = Pattern.compile("OBJECT\\((?<class>\\w+(\\.\\w+)*)\\)(?<nullable>\\?)?");
    private static final Pattern OBJECT_ARRAY_PATTERN = Pattern.compile("ARRAY\\(OBJECT\\((?<class>\\w+(\\.\\w+)*)\\)(?<nullable>\\?)?\\)(?<arrayNullable>\\?)?");

    private static Class<? extends Annotation>[] IGNORE_IN_METHODS = new Class[]{
            BuiltInStartTimestampMs.class, BuiltInStartNanoTime.class,
            BuiltInTimestampMs.class, BuiltInNanoTime.class,
            Pool.class, Result.class, DB.class
    };

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    public static List<Method> findInitMethods(@Nonnull Class<?> cls) throws IllegalArgumentException {
        return Arrays.stream(getUniqueDeclaredMethods(cls))
                .filter(((Predicate<Method>) ReflectionUtils::isPublicNonStatic)
                        .and(ReflectionUtils::isInitAnnotationPresent)
                        .and(ReflectionUtils::isVoid))
                .collect(Collectors.toList());
    }

    public static Method findComputeMethod(@Nonnull Class<?> cls) throws IllegalArgumentException {
        List<Method> methods = Arrays.stream(getUniqueDeclaredMethods(cls))
                .filter(((Predicate<Method>) ReflectionUtils::isPublicNonStatic)
                        .and(ReflectionUtils::isComputeAnnotationPresent)
                        .and(ReflectionUtils::isVoid))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Class %s does not contain any public void methods annotated with %s annotation.",
                            cls.getName(), Compute.class.getName())
            );
        } else if (methods.size() > 1) {
            throw new IllegalArgumentException(
                    String.format("Class %s contains multiple public void methods annotated with %s annotation.",
                            cls.getName(), Compute.class.getName())
            );
        }
        return methods.get(0);
    }

    public static Method findResultMethod(@Nonnull Class<?> cls) throws IllegalArgumentException {
        List<Method> methods = Arrays.stream(getUniqueDeclaredMethods(cls))
                .filter(((Predicate<Method>) ReflectionUtils::isPublicNonStatic)
                        .and(ReflectionUtils::isResultAnnotationPresent))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Class %s does not contain any public void methods annotated with %s annotation.",
                            cls.getName(), Result.class.getName())
            );
        } else if (methods.size() > 1) {
            throw new IllegalArgumentException(
                    String.format("Class %s contains multiple public void methods annotated with %s annotation.",
                            cls.getName(), Result.class.getName())
            );
        }
        return methods.get(0);
    }

    public static Method findResetMethod(@Nonnull Class<?> cls) throws IllegalArgumentException {
        List<Method> methods = Arrays.stream(getUniqueDeclaredMethods(cls))
                .filter(((Predicate<Method>) ReflectionUtils::isPublicNonStatic)
                        .and(ReflectionUtils::isResetAnnotationPresent)
                        .and(ReflectionUtils::isVoid))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Class %s does not contain any public void methods annotated with %s annotation.",
                            cls.getName(), Reset.class.getName())
            );
        } else if (methods.size() > 1) {
            throw new IllegalArgumentException(
                    String.format("Class %s contains multiple public void methods annotated with %s annotation.",
                            cls.getName(), Reset.class.getName())
            );
        }
        return methods.get(0);
    }

    public static int startTimeIndex(@Nonnull Method method) {
        return annotatedParameterIndex(method, BuiltInStartTimestampMs.class);
    }

    public static int startNanoTimeIndex(@Nonnull Method method) {
        return annotatedParameterIndex(method, BuiltInStartNanoTime.class);
    }

    public static int timestampIndex(@Nonnull Method method) {
        return annotatedParameterIndex(method, BuiltInTimestampMs.class);
    }

    public static int nanoTimeIndex(@Nonnull Method method) {
        return annotatedParameterIndex(method, BuiltInNanoTime.class);
    }

    public static int resultIndex(@Nonnull Method method) {
        return annotatedParameterIndex(method, Result.class);
    }

    public static int poolIndex(@Nonnull Method method) {
        return annotatedParameterIndex(method, Pool.class);
    }

    public static int dbIndex(@Nonnull Method method) {
        return annotatedParameterIndex(method, DB.class);
    }

    private static int annotatedParameterIndex(@Nonnull Method method, Class<? extends Annotation> annotation) {
        for (int i = 0; i < method.getParameters().length; i++) {
            if (method.getParameters()[i].isAnnotationPresent(annotation)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    public static boolean isBoolean(Method method) {
        return method.getReturnType() == boolean.class;
    }

    public static boolean isPublicNonStatic(Method method) {
        return Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers());
    }

    public static boolean isInitAnnotationPresent(Method method) {
        return method.isAnnotationPresent(Init.class);
    }

    public static boolean isComputeAnnotationPresent(Method method) {
        return method.isAnnotationPresent(Compute.class);
    }

    public static boolean isResultAnnotationPresent(Method method) {
        return method.isAnnotationPresent(Result.class);
    }

    public static boolean isResetAnnotationPresent(Method method) {
        return method.isAnnotationPresent(Reset.class);
    }

    public static List<Argument> introspectComputeMethod(Method method, List<GenericType> genericTypes) {
        List<Argument> result = new ArrayList<>(method.getParameters().length);
        String[] names = PARAMETER_NAME_DISCOVERER.getParameterNames(method);

        mainCycle:
        for (int i = 0; i < names.length; i++) {
            Parameter parameter = method.getParameters()[i];
            for (Class<? extends Annotation> annotationToIgnore : IGNORE_IN_METHODS) {
                if (parameter.isAnnotationPresent(annotationToIgnore))
                    continue mainCycle;
            }
            String name = names[i];
            result.add(introspectComputeParameter(parameter, name, genericTypes));
        }
        return result;
    }

    public static List<InitArgument> introspectInitMethod(Method method, List<GenericType> genericTypes) {
        List<InitArgument> result = new ArrayList<>(method.getParameters().length);
        String[] names = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        for (int i = 0; i < names.length; i++) {
            Parameter parameter = method.getParameters()[i];
            if (parameter.isAnnotationPresent(BuiltInStartTimestampMs.class))
                continue;
            String name = names[i];
            result.add(introspectInitParameter(parameter, name, genericTypes));
        }
        return result;
    }

    public static Argument introspectResultMethod(Method method, List<GenericType> genericTypes) {
        Class<?> type = method.getReturnType();
        Type typeAnnotation = method.getAnnotation(Type.class);
        DataType dt = null;
        GenericType genericType = null;
        if (typeAnnotation == null) {
            dt = extractType(method);
            genericType = null;
        } else {
            for (GenericType gType : genericTypes) {
                Pattern objectPattern = genericObjectPattern(gType);
                Matcher objectMatcher = objectPattern.matcher(typeAnnotation.value());
                if (objectMatcher.matches()) {
                    genericType = gType;
                    dt = new ClassDataType(objectMatcher.group("nullable") != null);
                    break;
                }
                Pattern arrayPattern = genericArrayPattern(gType);
                Matcher arrayMatcher = arrayPattern.matcher(typeAnnotation.value());
                if (arrayMatcher.matches()) {
                    genericType = gType;
                    dt = new ArrayDataType(arrayMatcher.group("arrayNullable") != null,
                            new ClassDataType(arrayMatcher.group("nullable") != null));
                    break;
                }
            }
            if (dt == null) {
                dt = extractType(method);
            }
        }
        return new Argument("return", dt, type, genericType);
    }

    public static Argument introspectComputeParameter(Parameter parameter, String parameterName, List<GenericType> genericTypes) {
        Class<?> type = parameter.getType();
        Arg arg = parameter.getAnnotation(Arg.class);
        Type typeAnnotation = parameter.getAnnotation(Type.class);
        DataType dt = null;
        GenericType genericType = null;
        if (typeAnnotation == null) {
            dt = extractType(parameter, false, false);
            genericType = null;
        } else {
            for (GenericType gType : genericTypes) {
                Pattern objectPattern = genericObjectPattern(gType);
                Matcher objectMatcher = objectPattern.matcher(typeAnnotation.value());
                if (objectMatcher.matches()) {
                    genericType = gType;
                    dt = new ClassDataType(objectMatcher.group("nullable") != null);
                    break;
                }
                Pattern arrayPattern = genericArrayPattern(gType);
                Matcher arrayMapper = arrayPattern.matcher(typeAnnotation.value());
                if (arrayMapper.matches()) {
                    genericType = gType;
                    dt = new ArrayDataType(objectMatcher.group("arrayNullable") != null,
                            new ClassDataType(objectMatcher.group("nullable") != null));
                    break;
                }
            }
            if (dt == null) {
                dt = extractType(parameter, typeAnnotation.value());
            }
        }
        String name;
        if (arg == null) {
            name = parameterName.toUpperCase();
        } else {
            name = (StringUtils.isEmpty(arg.name()) ? parameterName : arg.name()).toUpperCase();
        }
        return new Argument(name, dt, type, genericType);
    }

    public static InitArgument introspectInitParameter(Parameter parameter, String parameterName, List<GenericType> genericTypes) {
        Class<?> type = parameter.getType();
        Arg arg = parameter.getAnnotation(Arg.class);
        Type typeAnnotation = parameter.getAnnotation(Type.class);
        DataType dt = null;
        GenericType genericType = null;
        if (typeAnnotation == null) {
            dt = extractType(parameter, false, false);
            genericType = null;
        } else {
            for (GenericType gType : genericTypes) {
                Pattern objectPattern = genericObjectPattern(gType);
                Matcher objectMatcher = objectPattern.matcher(typeAnnotation.value());
                if (objectMatcher.matches()) {
                    genericType = gType;
                    dt = new ClassDataType(objectMatcher.group("nullable") != null);
                    break;
                }
                Pattern arrayPattern = genericArrayPattern(gType);
                Matcher arrayMapper = arrayPattern.matcher(typeAnnotation.value());
                if (arrayMapper.matches()) {
                    genericType = gType;
                    dt = new ArrayDataType(objectMatcher.group("arrayNullable") != null,
                            new ClassDataType(objectMatcher.group("nullable") != null));
                    break;
                }
            }
            if (dt == null) {
                dt = extractType(parameter, typeAnnotation.value(), false, false);
            }
        }
        String name;
        String defaultValue;
        if (arg == null) {
            name = parameterName.toUpperCase();
            defaultValue = null;
        } else {
            name = (StringUtils.isEmpty(arg.name()) ? parameterName : arg.name()).toUpperCase();
            defaultValue = StringUtils.isEmpty(arg.defaultValue()) ? null : arg.defaultValue();
        }
        return new InitArgument(name, dt, type, genericType, defaultValue);
    }

    private static DataType extractType(Parameter parameter, String typeString, boolean nullable, boolean elementNullable) {
        DataType dt = forName(typeString);
        if (dt != null)
            return dt;
        return extractType(parameter, nullable, elementNullable);
    }

    private static DataType extractType(Parameter parameter, String typeString) {
        return extractType(parameter, typeString, true, true);
    }

    private static DataType extractType(Parameter parameter, boolean nullable, boolean elementNullable) {
        return extractType(parameter.getType(), parameter, nullable, elementNullable);
    }

    private static DataType extractType(Parameter parameter) {
        return extractType(parameter.getType(), parameter, true, true);
    }

    private static DataType extractType(Class<?> type, AnnotatedElement annotatedElement, boolean nullable, boolean elementNullable) {
        if (annotatedElement.isAnnotationPresent(Bool.class)) {
            if (type == byte.class) {
                return TimebaseTypes.getBooleanType(nullable);
            } else if (type == ByteArrayList.class) {
                return TimebaseTypes.getBooleanArrayType(nullable, elementNullable);
            }
        }
        if (annotatedElement.isAnnotationPresent(TimeOfDay.class)) {
            if (type == int.class) {
                return TimebaseTypes.TIME_OF_DAY_CONTAINER.getType(nullable);
            } else if (type == IntegerArrayList.class) {
                return TimebaseTypes.TIME_OF_DAY_CONTAINER.getArrayType(nullable, elementNullable);
            }
        }
        if (type == long.class) {
            if (annotatedElement.isAnnotationPresent(TimestampMs.class)) {
                return TimebaseTypes.DATE_TIME_CONTAINER.getType(nullable);
            } else if (annotatedElement.isAnnotationPresent(Decimal.class)) {
                return TimebaseTypes.DECIMAL64_CONTAINER.getType(nullable);
            }
        }
        if (type == LongArrayList.class) {
            if (annotatedElement.isAnnotationPresent(TimestampMs.class)) {
                return TimebaseTypes.DATE_TIME_CONTAINER.getArrayType(nullable, elementNullable);
            } else if (annotatedElement.isAnnotationPresent(Decimal.class)) {
                return TimebaseTypes.DECIMAL64_CONTAINER.getArrayType(nullable, elementNullable);
            }
        }
        return extractType(type, nullable, elementNullable);
    }

    private static DataType extractType(Class<?> cls, boolean nullable, boolean elementNullable) {
        if (cls == boolean.class) {
            return TimebaseTypes.getBooleanType(false);
        } else if (cls == byte.class) {
            return TimebaseTypes.getIntegerDataType(1, nullable);
        } else if (cls == short.class) {
            return TimebaseTypes.getIntegerDataType(2, nullable);
        } else if (cls == int.class) {
            return TimebaseTypes.getIntegerDataType(4, nullable);
        } else if (cls == long.class) {
            return TimebaseTypes.getIntegerDataType(8, nullable);
        } else if (cls == float.class) {
            return TimebaseTypes.FLOAT32_CONTAINER.getType(nullable);
        } else if (cls == double.class) {
            return TimebaseTypes.FLOAT64_CONTAINER.getType(nullable);
        } else if (cls == char.class) {
            return TimebaseTypes.CHAR_CONTAINER.getType(nullable);
        } else if (cls == CharSequence.class || cls == StringBuilder.class) {
            return TimebaseTypes.UTF8_CONTAINER.getType(true);
        } else if (cls == ByteArrayList.class) {
            return TimebaseTypes.getIntegerArrayDataType(1, nullable, elementNullable);
        } else if (cls == ShortArrayList.class) {
            return TimebaseTypes.getIntegerArrayDataType(2, nullable, elementNullable);
        } else if (cls == IntegerArrayList.class) {
            return TimebaseTypes.getIntegerArrayDataType(4, nullable, elementNullable);
        } else if (cls == LongArrayList.class) {
            return TimebaseTypes.getIntegerArrayDataType(8, nullable, elementNullable);
        } else if (cls == FloatArrayList.class) {
            return TimebaseTypes.FLOAT32_CONTAINER.getArrayType(nullable, elementNullable);
        } else if (cls == DoubleArrayList.class) {
            return TimebaseTypes.FLOAT64_CONTAINER.getArrayType(nullable, elementNullable);
        } else if (cls == CharacterArrayList.class) {
            return TimebaseTypes.CHAR_CONTAINER.getArrayType(nullable, elementNullable);
        } else {
            throw new IllegalArgumentException("Cannot extract DataType from " + cls.getName());
        }
    }

    public static String extractId(Class<?> cls) {
        String name;
        if (cls.isAnnotationPresent(Function.class)) {
            name = cls.getAnnotation(Function.class).value();
        } else {
            throw new IllegalArgumentException(String.format("%s is not annotated nor with %s", cls.getName(), Function.class));
        }
        return StringUtils.isEmpty(name) ? cls.getSimpleName() : name;
    }

    public static String extractId(Method method) {
        String name;
        if (method.isAnnotationPresent(Function.class)) {
            name = method.getAnnotation(Function.class).value();
        } else {
            throw new IllegalArgumentException(String.format("%s is not annotated nor with %s", method.getName(),
                    Function.class));
        }
        return StringUtils.isEmpty(name) ? method.getName() : name;
    }

    public static List<GenericType> extractGenericTypes(Class<?> cls) {
        if (cls.isAnnotationPresent(Generic.class)) {
            return Arrays.stream(cls.getAnnotation(Generic.class).value())
                    .map(p -> new GenericType(p.name()))
                    .collect(Collectors.toList());
        } else if (cls.isAnnotationPresent(GenericParameter.class)) {
            return Collections.singletonList(new GenericType(cls.getAnnotation(GenericParameter.class).name()));
        } else {
            return Collections.emptyList();
        }
    }

    public static DataType extractType(Type typeAnnotation) {
        String type = typeAnnotation.value();
        DataType dt = forName(type);
        if (dt == null)
            throw new IllegalArgumentException("Unrecognized type: " + type);
        return dt;
    }

    public static String extractTypeName(Type typeAnnotation) {
        String type = typeAnnotation.value();
        Matcher matcher = OBJECT_ARRAY_PATTERN.matcher(type);
        if (matcher.matches()) {
            String className = matcher.group("class");
            try {
                Class<?> cls = Class.forName(className);
                return ObjectArrayList.class.getName() + "<" + cls.getName() + ">";
            } catch (ClassNotFoundException exc) {
                LOG.info().append("Class ").append(className).append(" not found, so type '")
                        .append(type).appendLast("' couldn't be parsed and created.");
            }
        }
        return null;
    }

    public static String extractTypeName(Method method) {
        if (method.isAnnotationPresent(Type.class)) {
            return extractTypeName(method.getAnnotation(Type.class));
        } else if (method.getReturnType() == boolean.class) {
            for (Parameter parameter : method.getParameters()) {
                if (parameter.isAnnotationPresent(Result.class)) {
                    if (parameter.isAnnotationPresent(Type.class)) {
                        return extractTypeName(parameter.getAnnotation(Type.class));
                    } else {
                        return null;
                    }
                }
            }
            throw new IllegalArgumentException(String.format("Method %s returns boolean, however no arguments " +
                    "annotated with %s annotation were found.", method, Result.class.getName()));
        } else {
            return null;
        }
    }

    public static DataType extractType(Method method) {
        if (method.isAnnotationPresent(Type.class)) {
            return extractType(method.getAnnotation(Type.class));
        } else if (method.getReturnType() == boolean.class) {
            for (Parameter parameter : method.getParameters()) {
                if (parameter.isAnnotationPresent(Result.class)) {
                    if (parameter.isAnnotationPresent(Type.class)) {
                        return extractType(parameter.getAnnotation(Type.class));
                    } else {
                        return extractType(parameter);
                    }
                }
            }
            throw new IllegalArgumentException(String.format("Method %s returns boolean, however no arguments " +
                    "annotated with %s annotation were found.", method, Result.class.getName()));
        } else {
            return extractType(method.getReturnType(), method, true, true);
        }
    }

    public static DataType forName(String name) {
        name = name.trim();

        DataType result = TimebaseTypes.TYPES_MAP.get(name);
        if (result != null)
            return result;

        return parseObjectType(name);
    }

    private static DataType parseObjectType(String s) {
        Matcher matcher = OBJECT_PATTERN.matcher(s);
        if (matcher.matches()) {
            boolean nullable = matcher.group("nullable") != null;
            String className = matcher.group("class");
            try {
                Class<?> cls = Class.forName(className);
                RecordClassDescriptor rcd = Introspector.createEmptyMessageIntrospector().introspectRecordClass(cls);
                return new ClassDataType(nullable, rcd);
            } catch (ClassNotFoundException exc) {
                LOG.info().append("Class ").append(className).append(" not found, so type '")
                        .append(s).appendLast("' couldn't be parsed and created.");
            } catch (Introspector.IntrospectionException e) {
                LOG.info().append("Error while introspecting class ").append(className)
                        .append(". Message: ").appendLast(e.getMessage());
            }
        }
        matcher = OBJECT_ARRAY_PATTERN.matcher(s);
        if (matcher.matches()) {
            boolean arrayNullable = matcher.group("arrayNullable") != null;
            boolean nullable = matcher.group("nullable") != null;
            String className = matcher.group("class");
            try {
                Class<?> cls = Class.forName(className);
                RecordClassDescriptor rcd = Introspector.createEmptyMessageIntrospector().introspectRecordClass(cls);
                return new ArrayDataType(arrayNullable, new ClassDataType(nullable, rcd));
            } catch (ClassNotFoundException exc) {
                LOG.info().append("Class ").append(className).append(" not found, so type '")
                        .append(s).appendLast("' couldn't be parsed and created.");
            } catch (Introspector.IntrospectionException e) {
                LOG.info().append("Error while introspecting class ").append(className)
                        .append(". Message: ").appendLast(e.getMessage());
            }
        }
        return null;
    }

    private static Pattern genericObjectPattern(GenericType genericType) {
        return Pattern.compile(String.format(GENERIC_OBJECT_PATTERN, genericType.getId()));
    }

    private static Pattern genericArrayPattern(GenericType genericType) {
        return Pattern.compile(String.format(GENERIC_ARRAY_PATTERN, genericType.getId()));
    }

}