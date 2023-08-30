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
package com.epam.deltix.data.stream;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;

public abstract class ChannelPreferences {

    /*
      Use raw messages (@see deltix.qsrv.hf.pub.RawMessage).
    */
    public boolean                  raw = false;

    /**
     * Loader for Timebase types.
     * <p>
     * Resolves a tickdb type to a class bound with it.
     * <code>null</code> value means the default loader.
     * Any code, which needs to get the field value, must use getter method to avoid <code>NullPointerException</code>.
     * </p>
     */
    public TypeLoader               typeLoader;

    public TypeLoader               getTypeLoader() {
        return typeLoader != null ? typeLoader : TypeLoaderImpl.DEFAULT_INSTANCE;
    }

    public ChannelPerformance       channelPerformance = ChannelPerformance.MIN_CPU_USAGE;

    public ChannelPerformance       getChannelPerformance() {
        return channelPerformance;
    }
}