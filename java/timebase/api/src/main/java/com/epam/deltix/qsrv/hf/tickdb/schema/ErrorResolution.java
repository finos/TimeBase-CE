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
package com.epam.deltix.qsrv.hf.tickdb.schema;

import javax.xml.bind.annotation.XmlElement;

public final class ErrorResolution {

    @XmlElement
    public String defaultValue;

    @XmlElement
    public Result result;

    public ErrorResolution() { // JAXB
    }

    protected ErrorResolution(String defaultValue, Result result) {
        this.defaultValue = defaultValue;
        this.result = result;
    }

    public static ErrorResolution       resolve(String defaultValue) {
        return new ErrorResolution(defaultValue, Result.Resolved);
    }

    public static ErrorResolution       ignore() {
        return new ErrorResolution(null, Result.Ignored);
    }

    public enum Result {
        Resolved, Ignored
    }
}