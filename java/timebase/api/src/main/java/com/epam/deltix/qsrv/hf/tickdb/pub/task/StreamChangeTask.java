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
package com.epam.deltix.qsrv.hf.tickdb.pub.task;

import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.util.time.Interval;
import com.epam.deltix.util.time.Periodicity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="StreamChangeTask")
public class StreamChangeTask extends SchemaChangeTask {

    @XmlElement
    public BufferOptions    bufferOptions;

    @XmlElement
    public int              df = StreamOptions.MAX_DISTRIBUTION;

    @XmlElement
    public String           name;

    @XmlElement
    public String           description;

    @XmlElement
    public boolean          ha;

    /**
     *  Stream periodicity, if known.
     */
    @XmlElement
    public Periodicity      periodicity = Periodicity.mkIrregular();
}