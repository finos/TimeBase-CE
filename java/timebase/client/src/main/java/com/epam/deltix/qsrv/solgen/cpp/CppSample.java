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
package com.epam.deltix.qsrv.solgen.cpp;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.Project;
import com.epam.deltix.qsrv.solgen.base.Sample;
import com.epam.deltix.qsrv.solgen.base.Source;
import com.epam.deltix.qsrv.solgen.base.StringSource;

import java.util.HashMap;
import java.util.Map;

public abstract class CppSample implements Sample {

    private static final String RUN_SAMPLE_TEMPLATE = "run_sample.sh-template";

    private Source runSampleScript;

    private final String functionName;
    private final String stream;

    public CppSample(String functionName, String stream) {
        this.functionName = functionName;
        this.stream = stream;

        Map<String, String> params = new HashMap<>();
        params.put("cpp.sample.name", functionName);
        runSampleScript = new StringSource(
            "../run_" + functionName + ".sh",
            SolgenUtils.convertLineSeparators(
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), RUN_SAMPLE_TEMPLATE, params),
                "\n"
            )
        );
    }

    public String functionName() {
        return functionName;
    }

    public String getStream() {
        return stream;
    }

    @Override
    public void addToProject(Project project) {
        if (project instanceof MakeProject) {
            project.addSource(runSampleScript);
        }
    }
}