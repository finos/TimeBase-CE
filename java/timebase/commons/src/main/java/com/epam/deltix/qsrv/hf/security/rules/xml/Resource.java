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
package com.epam.deltix.qsrv.hf.security.rules.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.epam.deltix.util.security.AccessControlRule.ResourceFormat;
import com.epam.deltix.util.security.AccessControlRule.ResourceType;

public class Resource {
    public Resource() {};

    public Resource(String value) {
        this.value = value;
    }

    @XmlAttribute(name = "type")
    public ResourceType type = ResourceType.Principal;

    @XmlAttribute(name = "format")
    public ResourceFormat format = ResourceFormat.Text;

    @XmlValue
    public String value;
}
