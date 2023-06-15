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
package com.epam.deltix.qsrv.hf.tickdb.pub.task;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.pub.md.SimpleClassSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;

/**
 * 
 */
@XmlRootElement(name="StreamCopyTask")
public class StreamCopyTask implements TransformationTask {

    @XmlElement
    public CopyStreamMetaDataChange change;

    @XmlElement
    public String[]                 sources;

    @XmlElement
    public String                   target;

    @XmlElement
    public long                     startTime;

    @XmlElement
    public long                     endTime;

    @XmlElement
    public boolean                  isBackground = true;

    @XmlElement
    public String[]                 subscribedEntities;

    @XmlElement
    public String[]                 subscribedTypeNames;

    public StreamCopyTask() { } // JAXB

    public StreamCopyTask(MetaDataChange change) {
        CopyStreamMetaDataChange streamMd = new CopyStreamMetaDataChange(change.getSource(),
                change.getTarget(),
                change.mapping);
        streamMd.changes = change.changes;
        this.change = streamMd;
    }

    public void                 invalidate(DXTickDB db) {

        final SimpleClassSet in = new SimpleClassSet ();
        for (String stream : sources) {
            DXTickStream tickStream = db.getStream(stream);
            in.addContentClasses (DXTickStream.getClassDescriptors(tickStream));
        }

        DXTickStream stream = db.getStream(target);
        
        final RecordClassSet out = new RecordClassSet();
        if (stream.isFixedType ())
            out.addContentClasses (stream.getFixedType ());
        else
            out.addContentClasses (stream.getPolymorphicDescriptors());

        change.invalidate(in, out);
    }

    public DXTickStream[]       getSources(DXTickDB db) {
        
        ArrayList<DXTickStream> list = new ArrayList<DXTickStream>();
        for (int i = 0; i < sources.length; i++)
            list.add(db.getStream(sources[i]));

        return list.toArray(new DXTickStream[list.size()]);
    }

    @Override
    public boolean              isBackground() {
        return isBackground;
    }
}