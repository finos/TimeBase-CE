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
