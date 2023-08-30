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
package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;

import javax.xml.bind.annotation.XmlElement;

public class QQLParameter {

    @XmlElement()
    public String name;

    @XmlElement()
    public String type; // StandardTypes

    @XmlElement()
    public String value;

    public QQLParameter() { // JAXB
    }

    public QQLParameter(String name, DataType dataType, Object value) {
        this.name = name;
        this.type = StandardTypes.toSimpleName(dataType);
        this.value = String.valueOf(value);
    }

    public QQLParameter(String name, String dataType, Object value) {
        this.name = name;
        this.type = dataType;
        this.value = String.valueOf(value);
    }

    public Parameter    toParameter() {
        Parameter p = new Parameter(name, StandardTypes.forName(type));

        if (value != null)
            p.value.writeString(value);
        else
            p.value.writeNull();
        return p;
    }
}