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
package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.codec.cg.CodecGenerator;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.io.BasicIOUtil;
import com.epam.deltix.util.lang.JavaCompilerHelper;
import com.epam.deltix.util.lang.JavaCompilerHelper.SpecialClassLoader;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: Jan 16, 2009
 * @author BazylevD
 */
public abstract class ClassCodecFactory {
    private static final Log LOG = LogFactory.getLog(ClassCodecFactory.class.getPackage().getName());
    private static final boolean VERBOSE = ! Boolean.getBoolean("quiet");

    private static final AtomicInteger id = new AtomicInteger(0);
    private static final boolean trace;
    private static final String compileDir;

    static {
        trace = System.getProperty("compile.trace", "false").equals("true");
        compileDir = System.getProperty("compile.dir", ".");
    }

    public static enum Type {
        BOUND_DECODER,
        BOUND_EXTERNAL_DECODER,
        BOUND_ENCODER,

        UNBOUND_DECODER,
        UNBOUND_ENCODER
    }

    @SuppressWarnings("unchecked")
    public static Class<FixedBoundEncoder> createFixedBoundEncoder(
            TypeLoader loader,
            RecordLayout layout,
            SpecialClassLoader classLoader
    ) {
        return (Class<FixedBoundEncoder>) getClazz(layout, classLoader, Type.BOUND_ENCODER);
    }

    @SuppressWarnings("unchecked")
    public static Class<FixedExternalDecoder> createFixedExternalDecoder(
            TypeLoader loader,
            RecordLayout layout,
            SpecialClassLoader classLoader
    ) {
        return (Class<FixedExternalDecoder>) getClazz(layout, classLoader, Type.BOUND_EXTERNAL_DECODER);
    }

    @SuppressWarnings("unchecked")
    public static Class<BoundDecoder> createFixedBoundDecoder(
            TypeLoader loader,
            RecordLayout layout,
            SpecialClassLoader classLoader
    ) {
        return (Class<BoundDecoder>) getClazz(layout, classLoader, Type.BOUND_DECODER);
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends UnboundDecoder> createFixedUnboundDecoder(
            RecordLayout layout
    ) {
        return (layout.getNonStaticFields() != null) ?
                (Class<UnboundDecoder>) getClazz(layout, null, Type.UNBOUND_DECODER) :
                EmptyUnboundDecoder.class;
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends FixedUnboundEncoder> createFixedUnboundEncoder(
            RecordLayout layout
    ) {
        return (layout.getNonStaticFields() != null) ?
                (Class<FixedUnboundEncoder>) getClazz(layout, null, Type.UNBOUND_ENCODER) :
                EmptyUnboundEncoder.class;
    }

    public static void dumpCode(RecordLayout layout, Type type, String outDirectory) throws IOException {
        final RecordClassDescriptor classDescriptor = layout.getDescriptor();
        final String className = getSimpleName(classDescriptor.getName()) + type + id.incrementAndGet();
        final String classSource = generateCode(className, layout, null, type);
        BasicIOUtil.writeTextFile(outDirectory + File.separatorChar + className +  ".java", classSource);
    }

    public static String getCodecName(RecordClassDescriptor rcd, Type type) {
        String sn = getSimpleName(rcd.getName());
        if (!StringUtils.isValidJavaIdOrKeyword(sn))
            sn = "Unknown";
        return getSimpleName(sn + type + id.incrementAndGet());
    }

    private synchronized static Class<?> getClazz(
            RecordLayout layout,
            SpecialClassLoader classLoader,
            Type type
    ) {
        try {
            final RecordClassDescriptor classDescriptor = layout.getDescriptor();

            final String className = getCodecName(classDescriptor, type);
//            final CodecCacheKey key = new CodecCacheKey( layout.getTargetClass().getName() , classDescriptor.getGuid() , type.toString());
//            CodecCacheValue value = codecCache.getCodec(key);
//            if (value == null)
//                className = getCodecName(classDescriptor, type)
//            else {
//                //  className = codec.fullName();
//                try {
//                    return classLoader.loadClass(value.getjClass().fullName());
//                } catch (ClassNotFoundException e ){
//                    className = value.getjClass().fullName();
//                }
//            }

            if (LOG.isTraceEnabled()) {
                if (layout.isBound())
                    LOG.trace("compile %s %s bound to %s").with(type).with(classDescriptor.getName()).with(layout.getTargetClass().getName());
                else
                    LOG.trace("compile %s %s").with(type).with(classDescriptor.getName());
            }


            final CodecGenerator gen = generate(className, layout, classLoader, type);

            if (LOG.isDebugEnabled()) {
                LOG.debug(Util.printStackTrace(Thread.currentThread().getStackTrace()));
                LOG.debug(CodecGenerator.toString(gen.getJClass()));
            }

            if (trace)
                trace(gen);

            final Class<?> msgClass = layout.getTargetClass();

            final CompilationUnit[] dependencies = gen.getDependencies();
            if (dependencies != null) {
                final Map<String, String> mapClassName2Code = new HashMap<>();
                mapClassName2Code.put(className, CodecGenerator.toString(gen.getJClass()));
                for (CompilationUnit dependency : dependencies) {
                    mapClassName2Code.put(dependency.getJClass().fullName(), CodecGenerator.toString(dependency.getJClass()));
                }
                final ClassLoader cl = classLoader != null ? classLoader : msgClass != null ? msgClass.getClassLoader() : ClassCodecFactory.class.getClassLoader();
                final Map<String, Class<?>> classes = new JavaCompilerHelper(cl).compileClasses(mapClassName2Code);
                //codecCache.cleanNotCompiledClasses();
                return classes.get(gen.getJClass().fullName());
            } else {
                final ClassLoader cl = classLoader != null ? classLoader : msgClass != null ? msgClass.getClassLoader() : ClassCodecFactory.class.getClassLoader();
                // TODO: refactor gen/cu: getSourceCode
                return new JavaCompilerHelper(cl).compileClass(className, CodecGenerator.toString(gen.getJClass()));
            }

//            final ObjectArrayList notCompiledClasses = codecCache.getNotCompiledClasses();
//            if (notCompiledClasses.size() > 0) {
//                final Map<String, String> mapClassName2Code = new HashMap<>();
//                for (int i = 0; i <notCompiledClasses.size(); i++ ) {
//                    CodecCacheKey cacheKey = (CodecCacheKey) notCompiledClasses.get(i);
//                    JClass jClass = codecCache.getCodec(cacheKey).getjClass();
//                    mapClassName2Code.put(jClass.fullName(), CodecGenerator.toString(jClass));
//                }
//
//                final Map<String, Class<?>> classes = new JavaCompilerHelper(classLoader).compileClasses(mapClassName2Code);
//                codecCache.cleanNotCompiledClasses();
//                return classes.get(gen.getJClass().fullName());
//            } else {
//
//                // TODO: refactor gen/cu: getSourceCode
//                return new JavaCompilerHelper(classLoader).compileClass(className, CodecGenerator.toString(gen.getJClass()));
//            }
        } catch (Exception e) {
            if (VERBOSE) {
                dump (e, layout);
            }
            throw new RuntimeException(e);
        }
    }

    private static String generateCode(String className, RecordLayout layout, SpecialClassLoader classLoader, Type type) {
        final CodecGenerator gen = generate(className, layout, classLoader, type);
        return CodecGenerator.toString(gen.getJClass());
    }

     private static CodecGenerator generate(String className, RecordLayout layout, SpecialClassLoader classLoader, Type type) {
        final CodecGenerator gen = new CodecGenerator(layout.getLoader(), classLoader);
        if (type == Type.BOUND_ENCODER)
            gen.generateEncoder(className, layout);
        else if (type == Type.BOUND_DECODER)
            gen.generateDecoder(className, false, layout);
        else if (type == Type.BOUND_EXTERNAL_DECODER)
            gen.generateDecoder(className, true, layout);
        else
            throw new UnsupportedOperationException(type.toString());


//        CodecCacheKey cacheKey = new CodecCacheKey(layout.getTargetClass().getName(), layout.getDescriptor().getGuid(), type.toString());
//        CodecCacheValue codec = codecCache.getCodec(cacheKey);
//        if (codec != null) {
//            gen.setJClass(codec.getjClass());
//            gen.setDependencies(codec.getDependencies());
//        } else {
//            if (type == Type.BOUND_ENCODER) {
//                codec = new CodecCacheValue(gen.generateEncoder(className, layout), gen.getDependencies());
//                codecCache.addCodec(cacheKey,codec);
//            }
//            else if (type == Type.BOUND_DECODER) {
//                codec = new CodecCacheValue(gen.generateDecoder(className, false, layout), gen.getDependencies());
//                codecCache.addCodec(cacheKey,codec);
//            }
//            else if (type == Type.BOUND_EXTERNAL_DECODER) {
//                codec = new CodecCacheValue(gen.generateDecoder(className, true, layout), gen.getDependencies());
//                codecCache.addCodec(cacheKey,codec);
//            }
//            else
//                throw new UnsupportedOperationException(type.toString());
//        }
        return gen;
    }

    private static void trace(CodecGenerator gen) throws IOException {
        BasicIOUtil.writeTextFile(compileDir + File.separatorChar + extractSimpleName(gen.getJClass().fullName()) + ".java", CodecGenerator.toString(gen.getJClass()));

        final CompilationUnit[] dependencies = gen.getDependencies();
        if (dependencies != null)
            for (CompilationUnit dependency : dependencies) {
                BasicIOUtil.writeTextFile(compileDir + File.separatorChar + extractSimpleName(dependency.getJClass().fullName()) + ".java", CodecGenerator.toString(dependency.getJClass()));
            }
    }

    private static String extractSimpleName(String fullName) {
        final int idx = fullName.lastIndexOf('.');
        return (idx != -1) ? fullName.substring(idx + 1) :
                fullName;
    }

    private static String getSimpleName(String className) {
        if (className == null)
            return "Undefined";
        else {
            final int idx = className.lastIndexOf('$');
            if (idx != -1)
                return className.substring(idx + 1);
            else
                return className.substring(className.lastIndexOf('.') + 1);
        }
    }

    private static void dump(Exception e, RecordLayout layout) {
        RecordClassDescriptor rcd = layout.getDescriptor();
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final HashSet<EnumClassDescriptor> enumDescriptors = new HashSet<EnumClassDescriptor>();
        do {
            for (DataField dataField : rcd.getFields()) {
                if (dataField.getType() instanceof EnumDataType)
                    enumDescriptors.add(((EnumDataType) dataField.getType()).descriptor);
            }
            rcd.dump(os);
        } while ((rcd = rcd.getParent()) != null);

        // dump enums
        for (EnumClassDescriptor enumDescriptor : enumDescriptors) {
            enumDescriptor.dump(os);
        }

        try {
            os.close();
        } catch (IOException e1) {
            LOG.error("Failed to dump RecordClassDescriptor: %s").with(e1);
        }
        final Class<?> clazz = layout.getTargetClass();
        if (clazz != null)
            LOG.error("Error compiling type \"%s\" bound to %s: %s\nDumping record class descriptor:\n%s").with(layout.getDescriptor().getName()).with(clazz.getName()).with(e.getMessage()).with(os.toString());
        else
            LOG.error("Error compiling type \"%s\": %s\nDumping record class descriptor:\n%s").with(layout.getDescriptor().getName()).with(e.getMessage()).with(os.toString());
    }
}