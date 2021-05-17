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

public enum Language {
    JAVA("-java", "java", "Java"),
    NET("-net", "net", ".NET"),
    PYTHON("-python", "python", "Python"),
    CPP("-cpp", "cpp", "c++"),
    GO("-go", "go", "GoLang");

    private final String cmdOption;
    private final String setOption;
    private final String title;

    Language(String cmdOption, String setOption, String title) {
        this.cmdOption = cmdOption;
        this.setOption = setOption;
        this.title = title;
    }

    public String getCmdOption() {
        return cmdOption;
    }

    public String getTitle() {
        return title;
    }

    public String getSetOption() {
        return setOption;
    }
}
