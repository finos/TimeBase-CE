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
package com.epam.deltix.qsrv.hf.pub.md;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.util.ArrayList;

/**
 *
 */
@XmlRootElement(name = "typeDef")
@XmlType(name = "typeDef")
public class ClassDescriptorArray {
    @XmlElement(name = "classDescriptor")
    private ClassDescriptor[] descriptors;

    /**
     *  Used by JAXB
     */
    ClassDescriptorArray () {
    }

    public ClassDescriptorArray(RecordClassDescriptor concrete, ClassSet<RecordClassDescriptor> sentRCS) {
        final ArrayList<ClassDescriptor> list = new ArrayList<>();

        final RecordClassSet rcs = new RecordClassSet();
        rcs.addContentClasses(concrete);
        // put concrete class unconditionally at the first position
        list.add(concrete);

        for (ClassDescriptor cd : rcs.getClassDescriptors()) {
            // it was inserted earlier
            if (cd != concrete)
                list.add(cd);
            else {
                // put depended classes only when they are absent in sentRCS
                if (!contains(cd, sentRCS))
                    list.add(cd);
            }
        }

        descriptors = list.toArray(new ClassDescriptor[list.size()]);
    }

    public static boolean contains(ClassDescriptor cd, ClassSet<RecordClassDescriptor> set) {

        ClassDescriptor[] classes = set.getClasses();

        for (int i = 0;  classes != null && i < classes.length; i++) {
            ClassDescriptor clazz = classes[i];
            if (clazz.getGuid().equals(cd.getGuid()))
                return true;
        }

        return false;
    }

    public ClassDescriptor[] getDescriptors() {
        return descriptors;
    }
}