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
package com.epam.deltix.qsrv.hf.pub.util;


import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.ExceptionHandler;

/**
 * Suppresses error messages when timebase stream contains extended version of message class (not available to bound codec)
 */
public class ParentIgnoringTypeLoader extends TypeLoaderImpl {
    public static final TypeLoader INSTANCE = new ParentIgnoringTypeLoader();

    private final ClassLoader loader = InstrumentMessage.class.getClassLoader();

    // silently use parent class of each message
    @Override
    public Class load(ClassDescriptor cd, ExceptionHandler handler) throws ClassNotFoundException {
        String javaClassName = cd.getName();
        if (javaClassName != null) {
            try {
                return loader.loadClass(javaClassName);
            } catch (ClassNotFoundException e) {
                if (cd instanceof RecordClassDescriptor) {
                    ClassDescriptor parent = ((RecordClassDescriptor) cd).getParent();
                    if (parent != null)
                        return load(parent);
                    else
                        return InstrumentMessage.class;
                }
            }
        }
        return super.load(cd, handler);
    }

    @Override
    public Class load(ClassDescriptor cd) throws ClassNotFoundException {
        return load(cd, (ExceptionHandler) null);
    }

}