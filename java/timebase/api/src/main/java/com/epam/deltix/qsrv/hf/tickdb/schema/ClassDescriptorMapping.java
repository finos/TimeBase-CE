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
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedUnboundEncoder;

class ClassDescriptorMapping {
    public RecordClassDescriptor        target;
    public RecordClassDescriptor        source;

    public final FieldMapping[]         mappings;

    public UnboundDecoder               decoder;
    public FixedUnboundEncoder          encoder;

    public boolean                      hasChanges = true;

    public ClassDescriptorMapping(RecordClassDescriptor source) {
        this.source = source;
        this.mappings = new FieldMapping[0];
    }

    ClassDescriptorMapping(RecordClassDescriptor source,
                           RecordClassDescriptor target,
                           SchemaMapping schemaMapping)
    {
        this.source = source;
        this.target = target;
        this.encoder = CodecFactory.INTERPRETED.createFixedUnboundEncoder(target);
        this.decoder = CodecFactory.COMPILED.createFixedUnboundDecoder(source);

        this.mappings = schemaMapping.getMappings(source, target);
    }

    ClassDescriptorMapping(ClassDescriptorChange change, SchemaMapping schemaMapping) {
        this(   (RecordClassDescriptor)change.getSource(),
                (RecordClassDescriptor)change.getTarget(),
                schemaMapping
        );

        for (FieldMapping mapping : mappings) {
            AbstractFieldChange[] fieldChanges = change.getFieldChanges(
                    mapping.source != null ? mapping.source.getField() : null,
                    mapping.target != null ? mapping.target.getField() : null);

            // assuming we ca have only 1 change
            for (AbstractFieldChange fieldChange : fieldChanges)
                mapping.resolution = fieldChange.resolution;
        }

        hasChanges = change.getChangeImpact() != SchemaChange.Impact.None;
    }

    public boolean      isValid() {
        return target != null && decoder != null;
    }
}