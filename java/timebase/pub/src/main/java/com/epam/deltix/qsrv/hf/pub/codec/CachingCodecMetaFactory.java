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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.util.lang.Factory;
import com.epam.deltix.util.lang.Util;

import java.util.*;

/**
 *
 */
public class CachingCodecMetaFactory extends CodecMetaFactory {

    private static class BoundKey {
        private final TypeLoader            loader;
        private final RecordClassDescriptor cd;
        private Class<?>                    boundClass = null;

        BoundKey (TypeLoader loader, RecordClassDescriptor cd) {
            this.loader = loader;
            this.cd = cd;
        }

        public Class<?> getBoundClass() throws ClassNotFoundException {
            if (boundClass == null)
                boundClass = loader.load(cd);

            return boundClass;
        }

        @Override
        public boolean  equals (Object obj) {
            if (obj == null || getClass () != obj.getClass ())
                return false;
            
            final       BoundKey other = (BoundKey) obj;

            return (this.cd.equals(other.cd) && (this.loader.equals(other.loader) || isTheSameClass(other)));
        }

        private boolean isTheSameClass(BoundKey other) {
            try {
                return Util.xequals(getBoundClass(), other.getBoundClass());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int      hashCode () {
            //return (loader.hashCode () * 59 + cd.hashCode ());
            return (cd.hashCode ());
        }

        @Override
        public String   toString () {
            return (cd.getName () + "#" + loader);
        }
    }

    private final CodecMetaFactory                          delegate;

    public CachingCodecMetaFactory (CodecMetaFactory delegate) {
        this.delegate = delegate;
    }

    private Map <BoundKey, Factory <FixedBoundEncoder>>     fbeCache = null;
    private Map <BoundKey, Factory <FixedExternalDecoder>>  fedCache = null;
    private Map <BoundKey, Factory <BoundDecoder>>          fbdCache = null;
    private Map <RecordClassDescriptor, Factory <UnboundDecoder>>      fudCache = null;
    private Map <RecordClassDescriptor, Factory <FixedUnboundEncoder>> fueCache = null;

    public synchronized void        clearCache () {
        if (fbeCache != null)
            fbeCache.clear ();

        if (fedCache != null)
            fedCache.clear ();

        if (fbdCache != null)
            fbdCache.clear ();

        if (fudCache != null)
            fudCache.clear ();

        if (fueCache != null)
            fueCache.clear ();
    }

    @Override
    public synchronized Factory <FixedBoundEncoder> createFixedBoundEncoderFactory (
        TypeLoader                          loader,
        RecordClassDescriptor               cd
    )
    {
        Factory <FixedBoundEncoder>         ret;
        BoundKey                            key = new BoundKey (loader, cd);

        if (fbeCache == null) {
            fbeCache = new HashMap <BoundKey, Factory <FixedBoundEncoder>> ();
            ret = null;
        }
        else
            ret = fbeCache.get (key);

        if (ret == null) {
            ret = delegate.createFixedBoundEncoderFactory (loader, cd);
            fbeCache.put (key, ret);
        }

        return (ret);
    }

    @Override
    public synchronized Factory <FixedExternalDecoder>   createFixedExternalDecoderFactory (
        TypeLoader                              loader,
        RecordClassDescriptor                   cd
    )
    {
        Factory <FixedExternalDecoder>      ret = null;
        BoundKey                            key = new BoundKey (loader, cd);

        if (fedCache == null)
            fedCache = new HashMap <BoundKey, Factory <FixedExternalDecoder>> ();
        else
            ret = fedCache.get (key);

        if (ret == null) {
            ret = delegate.createFixedExternalDecoderFactory (loader, cd);
            fedCache.put (key, ret);
        }

        return (ret);
    }

    @Override
    public synchronized Factory <BoundDecoder>           createFixedBoundDecoderFactory (
        TypeLoader                              loader,
        RecordClassDescriptor                   cd
    )
    {
        Factory <BoundDecoder>      ret = null;
        BoundKey                    key = new BoundKey (loader, cd);

        if (fbdCache == null)
            fbdCache = new HashMap <BoundKey, Factory <BoundDecoder>> ();
        else
            ret = fbdCache.get (key);

        if (ret == null) {
            ret = delegate.createFixedBoundDecoderFactory (loader, cd);
            fbdCache.put (key, ret);
        }

        return (ret);
    }

    @Override
    public synchronized Factory <UnboundDecoder>         createFixedUnboundDecoderFactory (
        RecordClassDescriptor                   cd
    )
    {
        Factory <UnboundDecoder>      ret;

        if (fudCache == null) {
            fudCache = new HashMap <RecordClassDescriptor, Factory <UnboundDecoder>> ();
            ret = null;
        }
        else
            ret = fudCache.get (cd);

        if (ret == null) {
            ret = delegate.createFixedUnboundDecoderFactory (cd);
            fudCache.put (cd, ret);
        }

        return (ret);
    }

    @Override
    public synchronized Factory <FixedUnboundEncoder>    createFixedUnboundEncoderFactory (
        RecordClassDescriptor                   cd
    )
    {
        Factory <FixedUnboundEncoder>      ret;

        if (fueCache == null) {
            fueCache = new HashMap <RecordClassDescriptor, Factory <FixedUnboundEncoder>> ();
            ret = null;
        }
        else
            ret = fueCache.get (cd);

        if (ret == null) {
            ret = delegate.createFixedUnboundEncoderFactory (cd);
            fueCache.put (cd, ret);
        }

        return (ret);
    }
}