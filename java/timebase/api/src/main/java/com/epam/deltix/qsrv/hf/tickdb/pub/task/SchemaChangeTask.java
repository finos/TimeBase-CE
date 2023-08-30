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

import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

@XmlRootElement(name="SchemaChangeTask")
public class SchemaChangeTask implements TransformationTask {

    @XmlElement
    public boolean background = true;

    @XmlElement
    public StreamMetaDataChange change;

    public SchemaChangeTask() { } // 4JAXB

    public SchemaChangeTask(StreamMetaDataChange change) {
        this.change = change;
    }

    public SchemaChangeTask(StreamMetaDataChange change, boolean background) {
        this.change = change;
        this.background = background;
    }

    @Override
    public boolean              isBackground() {
        return background;
    }

    public void                 setBackground(boolean value) {
        background = value;
    }

    public void                 invalidate(RecordClassSet set) {
        change.invalidate(set);
    }
}