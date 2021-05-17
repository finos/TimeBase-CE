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
package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.util.collections.Visitor;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.*;

@XmlRootElement(name = "ClassSet")
public class SimpleClassSet implements com.epam.deltix.qsrv.hf.pub.md.ClassSet<RecordClassDescriptor>, Serializable {

    @XmlElement(name = "classDescriptor")
    private HashSet<ClassDescriptor> classDescriptors;

    @XmlElement (name = "contentClass")
    private List<String> contentClasses; // define class descriptors order

    public SimpleClassSet() {
    }

    public SimpleClassSet(RecordClassDescriptor[] classes) {
        defineClasses(classes);
    }

    public void                         addContentClasses(RecordClassDescriptor... cds) {
        defineClasses(cds);
    }

    @Override
    public ClassDescriptor[]            getClasses() {
        return classDescriptors.toArray(new ClassDescriptor[classDescriptors.size()]);
    }

    public RecordClassDescriptor[] getContentClasses() {

        if (contentClasses == null)
            return null;

        List<RecordClassDescriptor>  result = new ArrayList<RecordClassDescriptor>(contentClasses.size());

        for (String guid : contentClasses) {
            for (ClassDescriptor descriptor : classDescriptors) {
                if (descriptor instanceof RecordClassDescriptor && guid.equals(descriptor.getGuid()))
                    result.add((RecordClassDescriptor) descriptor);
            }
        }

        return result.toArray(new RecordClassDescriptor[result.size()]);
    }

    private void                 defineClasses (RecordClassDescriptor[] cds) {

        if (contentClasses == null)
            contentClasses = new ArrayList <String> ();

        for (RecordClassDescriptor cd : cds) {
            if (!contentClasses.contains(cd.getGuid()))
                contentClasses.add (cd.getGuid());
        }

        final Queue<ClassDescriptor> addQueue = new ArrayDeque<ClassDescriptor>();

        for (ClassDescriptor cd : cds)
            addQueue.offer (cd);

        final Visitor<ClassDescriptor> adder =
            new Visitor <ClassDescriptor> () {
                @Override
                public boolean                      visit (ClassDescriptor cd) {
                    addQueue.offer (cd);
                    return (true);
                }
            };

        for (;;) {
            ClassDescriptor             cd = addQueue.poll ();

            if (cd == null)
                break;

            if (classDescriptors == null)
                classDescriptors = new HashSet<ClassDescriptor>();

            if (!classDescriptors.add (cd))
                continue;

            cd.visitDependencies (adder);
        }
    }
}
