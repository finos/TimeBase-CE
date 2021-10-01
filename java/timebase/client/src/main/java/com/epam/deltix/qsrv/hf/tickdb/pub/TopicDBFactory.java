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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;

/**
 *  Public methods for adding support of {@link TopicDB} to {@link TickDB}.
 */
@ParametersAreNonnullByDefault
public class TopicDBFactory {

    private static final String factoryClass = "com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicSupportWrapper";
    private static final String factoryMethod = "wrapStandalone";


    /**
     * Wraps provided DB instance to support {@link com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB}.
     * @param delegate backing instance for all functionality except topics
     * @return wrapped instance that supports topics
     */
    public static DXTickDB create(DXTickDB delegate) {
        try {
            Class<?> impl = TopicDBFactory.class.getClassLoader().loadClass(factoryClass); // using runtime class loader
            Method factoryMethod = impl.getDeclaredMethod(TopicDBFactory.factoryMethod, DXTickDB.class);
            return (DXTickDB) factoryMethod.invoke(null, delegate);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}