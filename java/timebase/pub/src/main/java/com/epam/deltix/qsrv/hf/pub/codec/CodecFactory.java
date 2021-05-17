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
package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

/**
 *  Factory for all kinds of codecs. This class is thread-safe.
 */
public class CodecFactory {
    private static final Log LOG = LogFactory.getLog(CodecFactory.class);

    public static LogLevel VALIDATION_LEVEL = LogLevel.TRACE;

    private final CodecMetaFactory      meta;

    public CodecFactory (CodecMetaFactory meta) {
        this.meta = meta;
    }

    public static final CodecFactory    INTERPRETED =
        new CodecFactory (InterpretingCodecMetaFactory.INSTANCE);
    
    public static final CodecFactory    COMPILED =
        new CodecFactory (CompiledCodecMetaFactory.INSTANCE);
    /**
     * Client code should use this value, when it chooses a codec   
     */
    public static final CodecChooseMode CHOOSE_MODE;

    static {
        final String value = System.getProperty("use.codecs");
        if("interpreted".equals(value))
            CHOOSE_MODE = CodecChooseMode.USE_INTERPRETED;
        else if("compiled".equals(value))
            CHOOSE_MODE = CodecChooseMode.USE_COMPILED;
        else {
            CHOOSE_MODE = CodecChooseMode.NO_RESTRICTION;
            if (value != null)
                LOG.warn("wrong \"use.codecs\" value: %s").with(value);
        }
    }

    public static CodecFactory          newCompiledCachingFactory () {
        return (new CodecFactory (new CachingCodecMetaFactory (CompiledCodecMetaFactory.INSTANCE)));
    }

    public static CodecFactory          newInterpretingCachingFactory () {
        return (new CodecFactory (new CachingCodecMetaFactory (InterpretingCodecMetaFactory.INSTANCE)));
    }

    public static CodecFactory          newCachingFactory () {
        return (CHOOSE_MODE == CodecChooseMode.USE_INTERPRETED) ? newInterpretingCachingFactory() : newCompiledCachingFactory();
    }

    public static boolean               useInterpretedCodecs (boolean interpretedIsDesired) {
        if (CHOOSE_MODE == CodecFactory.CodecChooseMode.USE_COMPILED)
            return (false);

        if (CHOOSE_MODE == CodecFactory.CodecChooseMode.USE_INTERPRETED || interpretedIsDesired)
            return (true);

        return (false);
    }

    public FixedBoundEncoder            createFixedBoundEncoder (
        TypeLoader                         loader,
        RecordClassDescriptor               classDescriptor
    )
    {
        return (meta.createFixedBoundEncoderFactory(loader, classDescriptor).create());
    }

    public FixedExternalDecoder         createFixedExternalDecoder (
        TypeLoader loader,
        RecordClassDescriptor               classDescriptor
    )
    {
        return (meta.createFixedExternalDecoderFactory(loader, classDescriptor).create());
    }

    public BoundDecoder                 createFixedBoundDecoder (
        TypeLoader                          loader,
        RecordClassDescriptor               classDescriptor
    )
    {
        return (meta.createFixedBoundDecoderFactory(loader, classDescriptor).create());
    }

    public UnboundDecoder               createFixedUnboundDecoder (
        RecordClassDescriptor               classDescriptor
    )
    {
        return (meta.createFixedUnboundDecoderFactory (classDescriptor).create ());
    }

    public FixedUnboundEncoder          createFixedUnboundEncoder (
        RecordClassDescriptor               classDescriptor
    )
    {
        return (meta.createFixedUnboundEncoderFactory (classDescriptor).create ());
    }

    public UnboundDecoder               createPolyUnboundDecoder (
        RecordClassDescriptor ...           classDescriptors
    )
    {
        return (meta.createPolyUnboundDecoderFactory (classDescriptors).create ());
    }

//    @Deprecated
//    public RandomAccessDecoder               createPolyRandomUnboundDecoder (
//        RecordClassDescriptor ...           classDescriptors
//    )
//    {
//        return (meta.createPolyUnboundDecoderFactory1 (classDescriptors).create ());
//    }

    public PolyUnboundEncoder           createPolyUnboundEncoder (
        RecordClassDescriptor ...           classDescriptors
    )
    {
        return (meta.createPolyUnboundEncoderFactory (classDescriptors).create ());
    }

    public BoundDecoder                 createPolyBoundDecoder (
        TypeLoader                         loader,
        RecordClassDescriptor ...           classDescriptors
    )
    {
        return (createPolyBoundDecoder (loader, false, classDescriptors));
    }

    public BoundDecoder                 createPolyBoundDecoder (
        TypeLoader                         loader,
        boolean                             ignoreUnloadableClasses,
        RecordClassDescriptor ...           classDescriptors
    )
    {
        return (meta.createPolyBoundDecoderFactory (loader, ignoreUnloadableClasses, classDescriptors).create ());
    }

    public PolyBoundEncoder             createPolyBoundEncoder (
        TypeLoader                         loader,
        RecordClassDescriptor ...           classDescriptors
    )
    {
        return (createPolyBoundEncoder (loader, false, classDescriptors));
    }

    public PolyBoundEncoder             createPolyBoundEncoder (
        TypeLoader                         loader,
        boolean                             ignoreUnloadableClasses,
        RecordClassDescriptor ...           classDescriptors
    )
    {
        return (meta.createPolyBoundEncoderFactory (loader, ignoreUnloadableClasses, classDescriptors).create ());
    }

//    public static RecordClassDescriptor getBuiltIn(RecordClassDescriptor rcd) {
//        do {
//            if (rcd.getJavaClassName() != null)
//                return rcd;
//        } while ((rcd = rcd.getParent()) != null);
//
//        return null;
//    }

    public static CodecFactory getDefault() {
        return get (CodecFactory.COMPILED);
    }

    public static CodecFactory get (boolean preferCompiled) {
        return get (preferCompiled ? CodecFactory.COMPILED : CodecFactory.INTERPRETED);
    }

    public static CodecFactory get (CodecFactory preferred) {
        return (CodecFactory.CHOOSE_MODE == CodecFactory.CodecChooseMode.USE_COMPILED) ? CodecFactory.COMPILED :
                (CodecFactory.CHOOSE_MODE == CodecFactory.CodecChooseMode.USE_INTERPRETED) ? CodecFactory.INTERPRETED : preferred;
    }


    public enum CodecChooseMode {
        /**
         * always use interpreted codec
         */
        USE_INTERPRETED,
        /**
         * always use compiled codec
         */
        USE_COMPILED,
        /**
         * client code is allowed to choose codec by itself
         */
        NO_RESTRICTION
    }

    public static void setValidationLevel(LogLevel level) {
        VALIDATION_LEVEL = level;
    }
}
