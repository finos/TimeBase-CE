/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.FunctionsRepo;
import com.epam.deltix.computations.api.annotations.MultiSignatureFunction;
import com.epam.deltix.computations.api.annotations.Signature;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.NamedObjectType;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.functions.InternalFunctions;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class StdEnvironment extends EnvironmentFrame {

    private static final Log LOG = LogFactory.getLog(StdEnvironment.class);

    private static final String CUSTOM_PACKAGES = System.getProperty("TimeBase.qql.functions.packages");

//    public static final EnumClassDescriptor INSTR_TYPE_ECD;
//    public static final EnumDataType INSTR_TYPE_ENUM;

    private final List<StatelessFunctionDescriptor> statelessFunctions = new ArrayList<>();
    private final List<StatefulFunctionDescriptor> statefulFunctions = new ArrayList<>();

//    static {
//        Introspector ix = Introspector.createEmptyMessageIntrospector();
//
//        try {
//            INSTR_TYPE_ECD = ix.introspectEnumClass(InstrumentType.class);
//        } catch (Introspector.IntrospectionException x) {
//            throw new RuntimeException("Error introspecting built-in types", x);
//        }
//
//        INSTR_TYPE_ENUM = new EnumDataType(false, INSTR_TYPE_ECD);
//    }

    public StdEnvironment(Environment parent) {
        super(parent);

        List<Object> params = new ArrayList<>(Arrays.asList(
            new TypeAnnotationsScanner(), new SubTypesScanner(), "com.epam.deltix.computations"
        ));
        addCustomPackages(params);
        Reflections reflections = new Reflections(params.toArray());

        register(StandardTypes.CLEAN_BOOLEAN);
        register(StandardTypes.CLEAN_BINARY);
        register(StandardTypes.CLEAN_CHAR);
        register(StandardTypes.CLEAN_FLOAT);
        register(StandardTypes.CLEAN_INTEGER);
        register(StandardTypes.CLEAN_TIMEOFDAY);
        register(StandardTypes.CLEAN_TIMESTAMP);
        register(StandardTypes.CLEAN_VARCHAR);

//        register(new ClassMap.EnumClassInfo(INSTR_TYPE_ECD));
//        QQLCompiler.setUpEnv(this, INSTR_TYPE_ECD);

        registerFunctionRepoClass(InternalFunctions.class);
        reflections.getTypesAnnotatedWith(FunctionsRepo.class)
                .forEach(this::registerFunctionRepoClass);
        reflections.getTypesAnnotatedWith(Function.class).forEach(this::registerFunction);

        bindPseudoFunction(QQLCompiler.KEYWORD_LAST);
        bindPseudoFunction(QQLCompiler.KEYWORD_FIRST);
        bindPseudoFunction(QQLCompiler.KEYWORD_REVERSE);
        bindPseudoFunction(QQLCompiler.KEYWORD_LIVE);
        bindPseudoFunction(QQLCompiler.KEYWORD_HYBRID);
        bindPseudoFunction(QQLCompiler.KEYWORD_POSITION);
        bindPseudoFunction(QQLCompiler.KEYWORD_NOW);
        bindPseudoFunction(QQLCompiler.KEYWORD_TYPE_OF);
        bindPseudoFunction(QQLCompiler.KEYWORD_TO_TIMESTAMP);
        bindPseudoFunction(QQLCompiler.KEYWORD_TO_TIMESTAMP_NS);
    }

    private void addCustomPackages(List<Object> params) {
        if (CUSTOM_PACKAGES != null) {
            String[] packages = CUSTOM_PACKAGES.split(",");
            for (String pack : packages) {
                pack = pack.trim();
                if (!pack.isEmpty()) {
                    params.add(pack);
                }
            }
        }
    }

    private void registerFunctionRepoClass(Class<?> clazz) {
        for (Method method : Arrays.stream(clazz.getMethods()).filter(StdEnvironment::isMethodMatches)
                .collect(Collectors.toList())) {
            registerFunction(clazz, method);
        }
    }

//    private void findFunctions() {
//        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
//        scanner.addIncludeFilter(new AnnotationTypeFilter(Signature.class));
//        scanner.addIncludeFilter(new AnnotationTypeFilter(FunctionsRepo.class));
//        scanner.addIncludeFilter(new AnnotationTypeFilter(Function.class));
//
//        for (BeanDefinition beanDefinition : scanner.findCandidateComponents("deltix")) {
//            try {
//                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
//                if (clazz.isAnnotationPresent(FunctionsRepo.class)) {
//                    for (Method method : Arrays.stream(clazz.getMethods()).filter(StdEnvironment::isMethodMatches)
//                            .collect(Collectors.toList())) {
//                        registerFunction(clazz, method);
//                    }
//                } else {
//                    registerFunction(clazz);
//                }
//                LOG.trace().append("Registered function ").appendLast(beanDefinition.getBeanClassName());
//            } catch (ClassNotFoundException e) {
//                LOG.trace().appendLast(e);
//            }
//        }
//    }

    private void bindPseudoFunction(String name) {
        bind(NamedObjectType.FUNCTION, name, name);
    }

    public final void register(DataType type) {
        bind(NamedObjectType.TYPE, type.getBaseName(), type);
    }

    public final void register(ClassMap.ClassInfo<?> ci) {
        bind(NamedObjectType.TYPE, ci.cd.getName(), ci);
    }

    public final void registerFunction(Class<?> cls) {
         if (cls.isAnnotationPresent(Function.class)) {
            LOG.trace().append("Registered function: ").appendLast(cls.getName());
            try {
                for (FunctionDescriptor functionDescriptor : FunctionDescriptor.create(cls)) {
                    register(functionDescriptor);
                }
            } catch (Exception exc) {
                LOG.warn().appendLast(exc);
            }
        }
    }

    public List<StatelessFunctionDescriptor> getStatelessFunctions() {
        return statelessFunctions;
    }

    public List<StatefulFunctionDescriptor> getStatefulFunctions() {
        return statefulFunctions;
    }

    public final void registerFunction(Class<?> cls, Method method) {
        LOG.trace().append("Registered function: ").appendLast(method.getName() + "." + method.getName());

        if (method.isAnnotationPresent(Function.class)) {
            SimpleFunctionDescriptor descriptor = SimpleFunctionDescriptor.create(cls, method);
            register(descriptor);
        } else {
            StaticMethodFunctionDescriptor[] descriptors = StaticMethodFunctionDescriptor.create(cls, method);
            for (StaticMethodFunctionDescriptor fd : descriptors) {
                register(fd);
            }
        }
    }

    private void register(StatefulFunctionDescriptor descriptor) {
        String id = descriptor.id();
        StatefulFunctionsSet set = (StatefulFunctionsSet) lookUpExactLocal(NamedObjectType.STATEFUL_FUNCTION, id);
        if (set == null) {
            set = new StatefulFunctionsSet(id);
            bind(NamedObjectType.STATEFUL_FUNCTION, id, set);
        }
        set.add(descriptor);
        statefulFunctions.add(descriptor);
    }

    private void register(StatelessFunctionDescriptor fd) {
        String id = fd.id();
        OverloadedFunctionSet ofs = (OverloadedFunctionSet)
                lookUpExactLocal(NamedObjectType.FUNCTION, id);
        if (ofs == null) {
            ofs = new OverloadedFunctionSet(id);

            bind(NamedObjectType.FUNCTION, id, ofs);
        }
        ofs.add(fd);
        statelessFunctions.add(fd);
    }

    private static boolean isMethodMatches(Method method) {
        return ((method.isAnnotationPresent(Signature.class) || method.isAnnotationPresent(MultiSignatureFunction.class)
                || method.isAnnotationPresent(Function.class)) && Modifier.isStatic(method.getModifiers()));
    }

}
