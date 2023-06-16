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
package com.epam.deltix.qsrv.solgen.java.samples;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ListStreamsSample implements JavaSample {

    public static final Property CLASS_NAME = PropertyFactory.create(
            "java.samples.liststreams.className",
            "Short class name like \"SampleClass\" (without package).",
            false,
            JavaSamplesUtil::isValidClassName,
            "ListStreams"
    );
    public static final Property PACKAGE_NAME = PropertyFactory.create(
            "java.samples.liststreams.packageName",
            "Package name like deltix.timebase.sample",
            false,
            JavaSamplesUtil::isValidPackageName,
            "deltix.timebase.sample"
    );

    public static final List<Property> PROPERTIES = List.of();

    private static final String LIST_STREAMS_TEMPLATE = "ListStreams.java-template";

    private final Source listStreamsSource;

    public ListStreamsSample(Properties properties) {
        this(properties.getProperty(JavaSampleFactory.TB_URL.getName()),
                properties.getProperty(PACKAGE_NAME.getName(), PACKAGE_NAME.getDefaultValue()),
                properties.getProperty(CLASS_NAME.getName(), CLASS_NAME.getDefaultValue()));
    }

    public ListStreamsSample(String tbUrl, String packageName, String className) {

        Map<String, String> params = new HashMap<>();
        params.put(JavaSampleFactory.TB_URL.getName(), tbUrl);
        params.put(PACKAGE_NAME.getName(), packageName);
        params.put(CLASS_NAME.getName(), className);

        listStreamsSource = new StringSource(packageName.replace('.', '/') + "/" + className + ".java",
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), LIST_STREAMS_TEMPLATE, params));
    }

    @Override
    public void addToProject(Project project) {
        project.addSource(listStreamsSource);
    }

    @Override
    public boolean generateBeans() {
        return false;
    }

    @Override
    public String key() {
        return null;
    }

    @Override
    public String tbUrl() {
        return null;
    }
}