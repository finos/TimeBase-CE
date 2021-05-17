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
package com.epam.deltix.qsrv.solgen.base;

import java.util.List;
import java.util.Properties;

public interface ProjectFactoryBase {

    List<String> listJavaProjectTypes();

    Project createJavaProject(String projectType, Properties properties);

    List<Property> getJavaProjectProps(String projectType);

    Project createNETProject(String projectType, Properties properties);

    List<String> listNETProjectTypes();

    List<Property> getNETProjectProps(String projectType);

    Project createPythonProject(String projectType, Properties properties);

    List<String> listPythonProjectTypes();

    List<Property> getPythonProjectProps(String projectType);

    Project createCppProject(String projectType, Properties properties);

    List<String> listCppProjectTypes();
    
    List<Property> getCppProjectProps(String projectType);

    Project createGoProject(String projectType, Properties properties);

    List<String> listGoProjectTypes();

    List<Property> getGoProjectProps(String projectType);

    default List<String> listProjectTypes(Language language) {
        switch (language) {
            case JAVA:
                return listJavaProjectTypes();
            case NET:
                return listNETProjectTypes();
            case PYTHON:
                return listPythonProjectTypes();
            case CPP:
                return listCppProjectTypes();
            case GO:
                return listGoProjectTypes();
            default:
                throw new UnsupportedOperationException();
        }
    }

    default Project create(Language language, String projectType, Properties properties) {
        switch (language) {
            case JAVA:
                return createJavaProject(projectType, properties);
            case NET:
                return createNETProject(projectType, properties);
            case PYTHON:
                return createPythonProject(projectType, properties);
            case CPP:
                return createCppProject(projectType, properties);
            case GO:
                return createGoProject(projectType, properties);
            default:
                throw new UnsupportedOperationException();
        }
    }

    default List<Property> getProperties(Language language, String projectType) {
        switch (language) {
            case JAVA:
                return getJavaProjectProps(projectType);
            case NET:
                return getNETProjectProps(projectType);
            case PYTHON:
                return getPythonProjectProps(projectType);
            case CPP:
                return getCppProjectProps(projectType);
            case GO:
                return getGoProjectProps(projectType);
            default:
                throw new UnsupportedOperationException();
        }
    }

}
