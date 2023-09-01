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
package com.epam.deltix.qsrv.solgen.python;

import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.base.*;
import com.epam.deltix.qsrv.solgen.java.JavaSampleFactory;

import java.util.*;

public class ListStreamsSample extends PythonSample {

    static final List<Property> PROPERTIES = Collections.emptyList();

    private static final String SAMPLE_NAME = "ListStreams";
    private static final String SCRIPT_NAME = SAMPLE_NAME + ".py";
    private static final String TEMPLATE = SAMPLE_NAME + ".python-template";

    private final Source listStreamsSource;

    public ListStreamsSample(Properties properties) {
        this(properties.getProperty(JavaSampleFactory.TB_URL.getName()));
    }

    public ListStreamsSample(String tbUrl) {
        Map<String, String> params = new HashMap<>();
        params.put(JavaSampleFactory.TB_URL.getName(), tbUrl);
        params.put(SAMPLE_NAME_PROP, SAMPLE_NAME);
        params.put(SCRIPT_NAME_PROP, SCRIPT_NAME);

        listStreamsSource = new StringSource(
            SCRIPT_NAME,
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), TEMPLATE, params)
        );

        generateLaunchers(params);
    }
//
//    private String generateLauncher(String scriptName, boolean windows) {
//        StringBuilder script = new StringBuilder ();
//
//        if (!windows) {
//            script.append ("#! /bin/sh\n\n");
//        } else {
//            script.append ("@echo off").append("\r\n");
//        }
//        script.append("python");
//        script.append(Util.IS_WINDOWS_OS ? " main.py " : " ./main.py ");
//        script.append(scriptName).append("\r\n");
//
//        if (!windows) {
//            script.append("if [ -z ${1+x} ] || [ \"$1\" != \"-force\" ]; then").append("\r\n");
//            script.append("read -p \"Press enter to exit\" nothing\n").append("\r\n");
//            script.append("fi");
//        } else {
//            script.append("if not \"%1\"==\"-force\" pause");
//        }
//
//        return script.toString();
//    }

    @Override
    public void addToProject(Project project) {
        super.addToProject(project);

        project.addSource(listStreamsSource);
    }

}