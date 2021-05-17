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

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.qsrv.hf.pub.*;

/**
 * Provides fields common for SelectionOptions and LoadingOptions 
 */
public abstract class CommonOptions extends ChannelPreferences {

    /**
     * Channel quality of service setting.
     */
    public ChannelQualityOfService  channelQOS = ChannelQualityOfService.MAX_THROUGHPUT;

    public ChannelCompression       compression = ChannelCompression.AUTO;

    public int                      channelBufferSize = 0; // 0 means default value

    protected void copy(CommonOptions template) {
        this.typeLoader = template.typeLoader;
        this.channelQOS = template.channelQOS;
        this.channelPerformance = template.channelPerformance;
        this.channelBufferSize = template.channelBufferSize;
    }
}
