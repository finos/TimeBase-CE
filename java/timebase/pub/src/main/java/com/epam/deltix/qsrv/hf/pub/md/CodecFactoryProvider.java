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

import com.epam.deltix.qsrv.hf.pub.codec.CachingCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory.CodecChooseMode;
import com.epam.deltix.qsrv.hf.pub.codec.CompiledCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.InterpretingCodecMetaFactory;

public class CodecFactoryProvider {
    private final CachingCodecMetaFactory intpFactoryCache = new CachingCodecMetaFactory(InterpretingCodecMetaFactory.INSTANCE);
    private final CodecFactory intpCodecFactory = new CodecFactory(intpFactoryCache);

    private final CachingCodecMetaFactory compFactoryCache = new CachingCodecMetaFactory(CompiledCodecMetaFactory.INSTANCE);
    private final CodecFactory compCodecFactory = new CodecFactory(compFactoryCache);

    private final CodecChooseMode codecChooseMode;

    private final CodecFactory defaultCodecFactory;

    public CodecFactoryProvider() {
        this(CodecFactory.CHOOSE_MODE);
    }

    public CodecFactoryProvider(CodecChooseMode codecChooseMode) {
        this.codecChooseMode = codecChooseMode;
        switch (codecChooseMode) {
            case USE_COMPILED:
                defaultCodecFactory = compCodecFactory;
                break;
            case USE_INTERPRETED:
                defaultCodecFactory = intpCodecFactory;
                break;
            default:
                defaultCodecFactory = compCodecFactory;
        }
    }

    public CodecFactory getDefault() {
        return defaultCodecFactory;
    }

    public CodecFactory get(boolean preferCompiled) {
        switch (codecChooseMode) {
            case USE_COMPILED:
            case USE_INTERPRETED:
                return defaultCodecFactory;
            default:
                return preferCompiled ? compCodecFactory : intpCodecFactory;
        }
    }
}