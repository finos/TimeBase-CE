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
package com.epam.deltix.qsrv.hf.tickdb.comm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class DefaultExceptionResolver implements ExceptionResolver {

    public static final DefaultExceptionResolver INSTANCE = new DefaultExceptionResolver();

    @Override
    public Throwable        create(Class<?> c, String message, Throwable cause) {
        Constructor<?> two = null;
        try {
            two = c.getConstructor(String.class, Throwable.class);
        } catch (NoSuchMethodException e) {
            // ignore
        }

        Constructor<?> one = null;
        try {
            one = c.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            // ignore
        }

        if (two == null && one == null)
            throw new IllegalStateException("Cannot find public constructors for the " + c.getName());

        Throwable x;
        try {
            if (cause != null && two != null)
                x = (Throwable) two.newInstance(message, cause);
            else
                x = (Throwable) one.newInstance(message);

        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return x;
    }
}
