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
package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.ExceptionHandler;

public class TypeLoaderImpl implements RegularTypeLoader, ExceptionHandler {
    private static final Log LOG = LogFactory.getLog(TypeLoaderImpl.class);
    public static final TypeLoader  DEFAULT_INSTANCE = new TypeLoaderImpl ();

    public static final TypeLoader  SILENT_INSTANCE = new TypeLoaderImpl () {
        @Override
        public void handle(Throwable x) {
            // suppress
        }

        @Override
        public void handle(ClassDescriptor cd, ClassNotFoundException e) {
            // suppress
        }
    };

    private final ClassLoader loader;

    public TypeLoaderImpl(ClassLoader loader) {
        this.loader = loader;
    }


    public TypeLoaderImpl () {
        this (TypeLoaderImpl.class.getClassLoader ());
    }


    public static Class load(ClassDescriptor cd, ClassLoader loader, ExceptionHandler handler) throws ClassNotFoundException {
        final String        javaClassName = cd.getName ();

        if (javaClassName == null)
            throw new ClassNotFoundException (
                "Class " + cd.getName () + " is not associated with a run-time class."
            );

        try {
            return loader.loadClass(javaClassName);
        } catch (ClassNotFoundException e) {

            if (handler != null)
                handler.handle(e);

            // for record class look up among parents
            if (cd instanceof RecordClassDescriptor) {
                final ClassDescriptor parent = ((RecordClassDescriptor) cd).getParent();
                if (parent == null)
                    return InstrumentMessage.class;
                else
                    return load(parent, loader, handler);
            } else
                throw e;
        }
    }

    @Override
    public void         handle(Throwable x) {
        LOG.warn("Bind error: type loader is unable to load class. (will try mapping parent): %s").with(x);
    }

    public void         handle(ClassDescriptor cd, ClassNotFoundException e) {
        LOG.warn("Bind error: type loader is unable to load class for the %s (will try mapping parent)").with(cd.getName());
    }

    @Override
    public Class        load (ClassDescriptor cd) throws ClassNotFoundException {
        return load(cd, loader, this);
    }

    @Override
    public Class<?>     load(ClassDescriptor cd, ExceptionHandler handler) throws ClassNotFoundException {
        return load(cd, loader, handler != null ? handler : this);
    }

    public ClassLoader  getClassLoader() {
        return loader;
    }

    @Override
    public boolean      equals (Object obj) {
        return (
            obj == this ||
            obj.getClass () == TypeLoaderImpl.class &&
            ((TypeLoaderImpl) obj).loader == loader
        );       
    }

    @Override
    public int          hashCode () {
        return loader.hashCode ();
    }

    @Override
    public String       toString () {
        return (super.toString () + " (" + loader + ")");
    }
}