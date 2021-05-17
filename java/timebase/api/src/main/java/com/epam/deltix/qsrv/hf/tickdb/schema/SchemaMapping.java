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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.codec.FieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.xml.MapAdaptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.*;

public class SchemaMapping {

    public SchemaMapping() { } // for jaxb

    @XmlElement
    @XmlJavaTypeAdapter(MapAdaptor.class)
    public final HashMap<String, String> descriptors = new HashMap<>();

    @XmlElement
    @XmlJavaTypeAdapter(MapAdaptor.class)    
    public final HashMap<DataField, DataField> fields = new HashMap<>();

    @XmlElement
    @XmlJavaTypeAdapter(MapAdaptor.class)
    public final HashMap<EnumValue, EnumValue> enumValues = new HashMap<>();

    
    public FieldLayout findField(FieldLayout source, RecordLayout target) {        

        for (Map.Entry<DataField, DataField> entry : fields.entrySet()) {
            if (SchemaAnalyzer.isEquals(entry.getKey(), source.getField()))
                return target.getField(entry.getValue().getName());
        }
        
        return target.getField(source.getField().getName());
    }

    public FieldLayout findField(RecordLayout source, FieldLayout target) {
        
        for (Map.Entry<DataField, DataField> entry : fields.entrySet()) {
            if (SchemaAnalyzer.isEquals(entry.getValue(), target.getField()))
                return source.getField(entry.getKey().getName());
        }

        return source.getField(target.getField().getName());
    }

    public ClassDescriptor findClassDescriptor(RecordClassDescriptor source,
                                               MetaData<RecordClassDescriptor> set) {
        return findClassDescriptor(source, set, false);

    }

    public ClassDescriptor findClassDescriptor(RecordClassDescriptor source,
                                                      MetaData<RecordClassDescriptor> set, boolean searchAll) {

        ClassDescriptor[] classes = searchAll ? set.getClasses() : set.getContentClasses();

        if (descriptors.containsKey(source.getGuid())) {
            String guid = descriptors.get(source.getGuid());
            for (ClassDescriptor rcd : classes) {
                if (Util.xequals(rcd.getGuid(), guid))
                    return rcd;
            }
        }

        List<ClassDescriptor> classList = Arrays.asList(classes);

        ClassDescriptor descriptor = set.getClassDescriptor(source.getName());
        if (descriptor != null) {
            if (searchAll)
                return descriptor;
            else if (classList.contains(descriptor))
                return descriptor;
        }

         // additional search in parent classes
        RecordClassDescriptor parent = source.getParent();
        while (parent != null) {
            descriptor = set.getClassDescriptor(parent.getName());
            if (searchAll)
                return descriptor;
            else if (classList.contains(descriptor))
                return descriptor;
            parent = parent.getParent();
        }

        // additional search in subclases
        for (ClassDescriptor rcd : classes) {
            parent = rcd instanceof RecordClassDescriptor ? ((RecordClassDescriptor)rcd).getParent() : null;
            while (parent != null) {
                if (source.getName().equals(parent.getName()))
                    return rcd;
                parent = parent.getParent();
            }
        }

        return null;
    }

    /*
        Sort target class descriptors according to order of source class descriptors
     */
    MetaData<RecordClassDescriptor> sort(
            MetaData<RecordClassDescriptor> source,
            MetaData<RecordClassDescriptor> target)
    {
        List<RecordClassDescriptor> sourceList = Arrays.asList(source.getContentClasses());
        RecordClassDescriptor[] targetClasses = target.getContentClasses();
        RecordClassDescriptor[] classes = new RecordClassDescriptor[sourceList.size()];

        List<RecordClassDescriptor> other = new ArrayList<RecordClassDescriptor>();

        for (RecordClassDescriptor descriptor : targetClasses) {
            ClassDescriptor match = findClassDescriptor(descriptor,  source);
            if (match instanceof RecordClassDescriptor) {
                int index = sourceList.indexOf((RecordClassDescriptor) match);
                if (index != -1 && classes[index] == null)
                    classes[index] = descriptor;
                else
                    other.add(descriptor);
            }
            else if (match == null)
                other.add(descriptor);
        }

        RecordClassSet result = new RecordClassSet();
        for (RecordClassDescriptor aClass : classes) {
            if (aClass != null)
                result.addContentClasses(aClass);
        }

        if (other.size() > 0)
            result.addContentClasses(other.toArray(new RecordClassDescriptor[other.size()]));

        // process non-content classes
        List<RecordClassDescriptor> targetList = Arrays.asList(target.getContentClasses());

        for (ClassDescriptor cd : target.getClassDescriptors()) {
            if (cd instanceof RecordClassDescriptor) {
                if (!targetList.contains((RecordClassDescriptor)cd))
                    result.addClasses(cd);
            } else {
                result.addClasses(cd);
            }
        }        

        return result;
    }

    public FieldMapping[] getMappings(RecordClassDescriptor in, RecordClassDescriptor out)
    {
        ArrayList<FieldMapping> mappings = new ArrayList<FieldMapping>();

        RecordLayout sourceLayout = new RecordLayout(in);
        RecordLayout targetLayout = new RecordLayout(out);

        NonStaticFieldLayout[] sourceFields = sourceLayout.getNonStaticFields();
        NonStaticFieldLayout[] targetFields = targetLayout.getNonStaticFields();        

        for (int i = 0; targetFields != null && i < targetFields.length; i++) {
            NonStaticFieldLayout target = targetFields[i];
            FieldLayout match = findField(sourceLayout, target);
            FieldMapping mapping;

            if (match instanceof NonStaticFieldLayout) {
                NonStaticFieldLayout source = (NonStaticFieldLayout) match;
                mapping = new FieldMapping(target, source);
                mapping.targetIndex = i;
                mapping.sourceIndex = Util.indexOf(sourceFields, match);
            }
            else {
                mapping = new FieldMapping(target, null);
                mapping.targetIndex = i;
                mapping.sourceIndex = -1;
            }

            mappings.add(mapping);
        }

        return mappings.toArray(new FieldMapping[mappings.size()]);
    }    
}
