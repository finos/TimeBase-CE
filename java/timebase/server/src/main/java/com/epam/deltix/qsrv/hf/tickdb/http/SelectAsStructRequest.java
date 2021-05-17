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
package com.epam.deltix.qsrv.hf.tickdb.http;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 *
 */
@XmlRootElement(name = "asStruct")
public class SelectAsStructRequest extends DownloadRequest {
    static int SIZE_1MB = 0x100000; // 1MB (must feet L3 CPU cache)

    /** Stream key */
    @XmlElement()
    public String stream;

    @XmlElement()
    @XmlJavaTypeAdapter(IdentityKeyListAdapter.class)
    public IdentityKey[] instruments;

    @XmlElement()
    public int symbolLength = 10;

    @XmlElement(name = "type")
    public RecordType[] types;

    @XmlElementWrapper
    @XmlElement(name = "item")
    public String[] concreteTypes;

    @XmlElement
    public int bufferSize = SIZE_1MB;
}
