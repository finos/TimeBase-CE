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

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

import java.util.Optional;

@XmlRootElement(name="SchemaUpdateTask")
public class SchemaUpdateTask implements TransformationTask {

    @XmlElement()
    public boolean              polymorphic;

    @XmlElement()
    public String               schema;

    /*
     *  Default values for the fields (if required), field name included fully qualified class name (ClassName:FieldName)
     */
    @XmlElement()
    public Elements defaults;

    /*
     *  Mappings for the changed classes and fields
     */
    @XmlElement()
    public Elements mappings;

    @XmlElement()
    public boolean              background = false;

    @Override
    public boolean              isBackground() {
        return background;
    }

    public SchemaChangeTask     getChanges(DXTickStream stream) {
        RecordClassSet classSet = TDBProtocol.readClassSet(schema);

        RecordClassSet source = new RecordClassSet ();
        MetaDataChange.ContentType  inType;

        if (stream.isFixedType ()) {
            inType = MetaDataChange.ContentType.Fixed;
            source.addContentClasses (stream.getFixedType ());
        } else {
            inType = MetaDataChange.ContentType.Polymorphic;
            source.addContentClasses (stream.getPolymorphicDescriptors ());
        }

        MetaDataChange.ContentType  outType = polymorphic ? MetaDataChange.ContentType.Polymorphic : MetaDataChange.ContentType.Fixed;

        SchemaMapping mapping = new SchemaMapping();

        // process mapping for descriptors and fields

        if (mappings != null && mappings.items != null) {
            for (MapElement entry : mappings.items) {
                String[] from = entry.name.split(":");

                if (from.length == 2) { // DataField
                    DataField fromField = source.findField(from[0], from[1]);
                    String[] to = entry.value.split(":");
                    DataField toField = classSet.findField(to[0], to[1]);

                    mapping.fields.put(fromField, toField);

                } else if (from.length == 1) { // CD
                    mapping.descriptors.put(entry.name, entry.value);
                } else {
                    throw new IllegalStateException("Unknown name: " + entry.name);
                }
            }
        }

        StreamMetaDataChange changes = new SchemaAnalyzer(mapping).getChanges(source, inType, classSet, outType);

        if (defaults != null && defaults.items != null) {
            for (MapElement entry : defaults.items) {
                String[] from = entry.name.split(":");

                if (from.length == 2) { // DataField
                    RecordClassDescriptor cd = MetaDataChange.getClassDescriptor(source, from[0]);

                    ClassDescriptorChange change = changes.getChange(cd, null);
                    if (change != null && cd != null) {
                        AbstractFieldChange[] fieldChanges = change.getFieldChanges(cd.getField(from[1]), null);

                        for (AbstractFieldChange c : fieldChanges) {

                            String fullName = cd.getName() + " [" + c.getSource().getName() + "]";

                            if (c.hasErrors()) {
                                String value = entry.value;

                                if (c instanceof FieldTypeChange) {
                                    ((FieldTypeChange) c).setDefaultValue(value);
                                    if (c.hasErrors())
                                        throw new IllegalStateException(fullName + ": default value expected.");
                                } else if (c instanceof CreateFieldChange) {
                                    ((CreateFieldChange) c).setInitialValue(value);
                                    if (c.hasErrors())
                                        throw new IllegalStateException(fullName + ": default value expected.");
                                } else if (c instanceof FieldModifierChange) {
                                    if (c.getTarget() instanceof StaticDataField)
                                        ((FieldModifierChange) c).setInitialValue(((StaticDataField) c.getTarget()).getStaticValue());
                                    else
                                        ((FieldModifierChange) c).setInitialValue(value);

                                    if (c.hasErrors())
                                        throw new IllegalStateException(fullName + ": default value expected.");
                                }
                            }
                        }

                    }

                } else {
                    throw new IllegalStateException("Unknown field reference: " + entry.value);
                }
            }
        }

        SchemaChangeTask task = new SchemaChangeTask(changes);
        task.setBackground(background);

        return task;
    }

    public void    addMapping(String name, String value) {
        if (mappings == null)
            mappings = new Elements(new ArrayList<>());
        mappings.items.add(new MapElement(name, value));
    }

    public void    addDefault(String name, String value) {
        if (defaults == null)
            defaults = new Elements(new ArrayList<>());
        defaults.items.add(new MapElement(name, value));
    }

    public String   getMapping(String name) {
        if (mappings != null) {
            Optional<MapElement> element = mappings.items.stream().filter(x -> x.name.equals(name)).findFirst();
            if (element.isPresent())
                return element.get().value;
        }

        return null;
    }

    public String   getDefault(String name) {

        if (defaults != null) {
            Optional<MapElement> element = defaults.items.stream().filter(x -> x.name.equals(name)).findFirst();
            if (element.isPresent())
                return element.get().value;
        }

        return null;
    }
}
