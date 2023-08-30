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
package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.time.Interval;

/**
 * Message file metadata container.
 */
public class MessageFileHeader {

    private final ClassSet              classes;
    public final int                    version;
    public Interval                     periodicity;

    public MessageFileHeader(int version, ClassSet set, Interval periodicity) {
        this.version = version;
        this.classes = set;
        this.periodicity = periodicity;
    }

    public MessageFileHeader(int version, RecordClassDescriptor[] descriptors, Interval periodicity) {
        this.version = version;
        this.periodicity = periodicity;
        (this.classes = new ClassSet()).addContentClasses(descriptors);
    }

    public static MessageFileHeader                         migrate(MessageFileHeader header) {
        return header;
//        try {
//            return new MessageFileHeader(header.version, new SchemaUpdater(new ClassMappings()).update(header.classes), header.periodicity);
//        } catch (ClassNotFoundException | Introspector.IntrospectionException e) {
//            throw new RuntimeException(e);
//        }
    }

    public RecordClassDescriptor[]      getTypes() {
        return classes.getContentClasses();
    }
    
    public ClassDescriptor[]            getAllTypes() {
        return classes.getClasses();
    }
}