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
package com.epam.deltix.qsrv.solgen.net.samples;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.net.NetSampleFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class WriteStreamSample implements Sample {

    public static final Property STREAM_KEY = PropertyFactory.create(
            "timebase.stream",
            "TimeBase stream key.",
            true,
            StringUtils::isNotEmpty
    );
    public static final List<Property> PROPERTIES = Collections.unmodifiableList(Collections.singletonList(STREAM_KEY));

    private static final String WRITE_STREAM_TEMPLATE = "WriteStream.cs-template";

    private final Source writeStreamSource;

    public WriteStreamSample(Properties properties) {
        this(properties.getProperty(NetSampleFactory.TB_URL.getName()),
                properties.getProperty(STREAM_KEY.getName()));
    }

    public WriteStreamSample(String tbUrl, String key) {
        Map<String, String> params = new HashMap<>();
        params.put(NetSampleFactory.TB_URL.getName(), tbUrl);
        params.put(STREAM_KEY.getName(), key);

        writeStreamSource = new StringSource("WriteStream.cs",
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), WRITE_STREAM_TEMPLATE, params));
    }

    @Override
    public void addToProject(Project project) {
        project.addSource(writeStreamSource);
    }
}
