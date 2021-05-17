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
package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectList;

public class SchemaChangeMessageHelper {

    public static StreamMetaDataChange getStreamMetaDataChange(SchemaChangeMessage message) {
        RecordClassSet target = MessageToDescriptorMapper.convert(message.getNewState());
        RecordClassSet source = MessageToDescriptorMapper.convert(message.getPreviousState());
        SchemaMapping mapping = new SchemaMapping();
        correctMapping(mapping, source, target, message);

        return new SchemaAnalyzer(mapping).getChanges(
                source,
                source.getNumTopTypes() > 1 ? MetaDataChange.ContentType.Polymorphic : MetaDataChange.ContentType.Fixed,
                target,
                target.getNumTopTypes() > 1 ? MetaDataChange.ContentType.Polymorphic : MetaDataChange.ContentType.Fixed
        );
    }

    private static SchemaMapping correctMapping(SchemaMapping mapping, RecordClassSet source, RecordClassSet target, SchemaChangeMessage message) {
        ObjectArrayList<SchemaDescriptorChangeActionInfo> descriptorChangeActions = message.getDescriptorChangeActions();
        descriptorChangeActions.forEach(change -> {
            ClassDescriptorInfo newState = change.getNewState();
            ClassDescriptorInfo previousState = change.getPreviousState();
            switch (change.getChangeTypes()) {
                case FIELDS_CHANGE:
                    ObjectList<SchemaFieldChangeActionInfo> fieldActions = change.getFieldChangeActions();
                    for (int i = 0; i < fieldActions.size(); i++) {
                        SchemaFieldChangeActionInfo fieldAction = fieldActions.get(i);
                        switch (fieldAction.getChangeTypes()) {
                            case RENAME:
                                fieldAction.getNewState();
                                mapping.fields.put(
                                        MessageToDescriptorMapper.map(fieldAction.getPreviousState(), message.getPreviousState()),
                                        MessageToDescriptorMapper.map(fieldAction.getNewState(), message.getNewState())
                                );
                        }
                    }
                    break;
                case RENAME:
                    ClassDescriptor targetDescriptor = target.getClassDescriptor(newState.getName().toString());
                    ClassDescriptor sourceDescriptor = source.getClassDescriptor(previousState.getName().toString());
                    mapping.descriptors.put(sourceDescriptor.getGuid(), targetDescriptor.getGuid());
                    break;
            }
        });
        return mapping;
    }
}
