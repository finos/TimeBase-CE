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
