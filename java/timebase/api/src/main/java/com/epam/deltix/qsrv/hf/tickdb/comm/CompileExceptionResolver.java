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
package com.epam.deltix.qsrv.hf.tickdb.comm;

import com.epam.deltix.util.parsers.CompilationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class CompileExceptionResolver extends DefaultExceptionResolver {

    private long errorLocation;

    public CompileExceptionResolver(long errorLocation) {
        this.errorLocation = errorLocation;
    }

    @Override
    public Throwable create(String className, String message, Throwable cause) {

        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        if (CompilationException.class.isAssignableFrom(clazz)) {
            Constructor<?> c = null;
            try {
                c = clazz.getConstructor(String.class, long.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(e);
            }

            Throwable x;
            try {
               x = (Throwable) c.newInstance(message, errorLocation);

            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }

            return x;
        }

        return super.create(className, message, cause);
    }
}