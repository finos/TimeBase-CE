package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;


public final class CodecCache {

    private ObjectToObjectHashMap<CodecCacheKey, CodecCacheValue> codecs;


    public CodecCache() {
        codecs = new ObjectToObjectHashMap<>();
        notCompiledClasses = new ObjectArrayList<>();
    }



    public CodecCacheValue getCodec(CodecCacheKey key){
        return codecs.get(key, null);
    }

    public void addCodec(CodecCacheKey key, CodecCacheValue value) {
        notCompiledClasses.add(key);
        codecs.put(key,value);
    }



    public void cleanNotCompiledClasses() {
        notCompiledClasses.clear();
    }

    public ObjectArrayList getNotCompiledClasses() {
        return notCompiledClasses;
    }

    private ObjectArrayList<CodecCacheKey> notCompiledClasses;


}
