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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.FieldIdentifier;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.NamedObjectType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TypeIdentifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.lookUpField;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.lookUpType;

/**
 *
 */
public final class ClassMap {
    public static abstract class ClassInfo <T extends ClassDescriptor> {
        public final EnvironmentFrame               fieldEnv;
        public final T                              cd;
        
        protected ClassInfo (T cd, EnvironmentFrame fieldEnv) {
            this.fieldEnv = fieldEnv;
            this.cd = cd;
            
            QQLCompiler.setUpEnv (fieldEnv, cd);
        }
    }
    
    public static class EnumClassInfo extends ClassInfo <EnumClassDescriptor> {
        public EnumClassInfo (EnumClassDescriptor ecd) {
            super (ecd, new EnvironmentFrame ());                             
        }
        
        public EnumValueRef                         lookUpValue (
            FieldIdentifier                             fieldId
        )
        {
            return ((EnumValueRef) lookUpField (fieldEnv, fieldId));            
        }
    }
    
    public static class RecordClassInfo extends ClassInfo <RecordClassDescriptor> {
        public final RecordClassInfo                parent;
        public final Set <RecordClassInfo>          directSubclasses =
            new HashSet <RecordClassInfo> ();
        
        public RecordClassInfo (RecordClassInfo parent, RecordClassDescriptor rcd) {
            super (
                rcd,
                parent == null ?
                    new EnvironmentFrame () :
                    new EnvironmentFrame (parent.fieldEnv)
            );

            this.parent = parent;                         
        }

        public DataFieldRef                         lookUpField (
            FieldIdentifier                             fieldId
        )
        {
            return ((DataFieldRef) QQLCompiler.lookUpField (fieldEnv, fieldId));
        }
    }

    private final Map <ClassDescriptor, ClassInfo>    infoMap =
        new HashMap <ClassDescriptor, ClassInfo> ();

    private final EnvironmentFrame                    typeEnv;

    public ClassMap (Environment parent) {
        typeEnv = new EnvironmentFrame (parent);
    }
    
    public ClassInfo                            lookUpClass (TypeIdentifier typeId) {
        return ((ClassInfo) lookUpType (typeEnv, typeId));
    }

    public ClassInfo                            lookUpClass (String typeName) {
        return ((ClassInfo) lookUpType (typeEnv, typeName));
    }

    public void                                 register (ClassDescriptor cd) {
        if (cd instanceof RecordClassDescriptor)
            registerClass ((RecordClassDescriptor) cd);
        else if (cd instanceof EnumClassDescriptor)
            registerEnum ((EnumClassDescriptor) cd);
        else
            throw new IllegalArgumentException (cd.toString ());
    }
    
    public EnumClassInfo                        registerEnum (EnumClassDescriptor ecd) {
        EnumClassInfo                ei = (EnumClassInfo) infoMap.get (ecd);
        
        if (ei != null)
            return (ei);
        
        ei = new EnumClassInfo (ecd);
        
        typeEnv.bind (NamedObjectType.TYPE, ecd.getName (), ei);
        infoMap.put (ecd, ei);
        
        return (ei);
    }
    
    public RecordClassInfo                      registerClass (RecordClassDescriptor rcd) {
        RecordClassInfo ci = (RecordClassInfo) infoMap.get(rcd);

        if (ci != null)
            return (ci);

        RecordClassDescriptor parentRCD = rcd.getParent();
        RecordClassInfo pci = parentRCD == null ? null : registerClass(parentRCD);

        ci = new RecordClassInfo(pci, rcd);

        if (pci != null)
            pci.directSubclasses.add(ci);

        typeEnv.bind(NamedObjectType.TYPE, rcd.getName(), ci);
        String shortName = shortName(rcd.getName());
        if (shortName != null) {
            typeEnv.bind(NamedObjectType.TYPE, shortName, ci);
        }
        infoMap.put(rcd, ci);

        for (DataField field : rcd.getFields()) {
            if (field.getType() instanceof ClassDataType) {
                for (RecordClassDescriptor descriptor : ((ClassDataType) field.getType()).getDescriptors()) {
                    registerClass(descriptor);
                }
            } else if (field.getType() instanceof ArrayDataType
                    && ((ArrayDataType) field.getType()).getElementDataType() instanceof ClassDataType) {
                for (RecordClassDescriptor descriptor : ((ClassDataType) ((ArrayDataType) field.getType())
                        .getElementDataType()).getDescriptors()) {
                    registerClass(descriptor);
                }
            }
        }

        return (ci);
    }

    private static String shortName(String name) {
        if (name == null)
            return null;
        int i = name.lastIndexOf(".");
        return i == -1 || i == name.length() ? null: name.substring(i + 1);
    }
    
    public Set <RecordClassInfo>                getDirectSubclasses (RecordClassDescriptor rcd) {
        RecordClassInfo               ci = (RecordClassInfo) infoMap.get (rcd);

        if (ci == null)
            return (null);

        return (Collections.unmodifiableSet (ci.directSubclasses));
    }   
    
    public Set <ClassDescriptor>                getAllDescriptors () {
        return (infoMap.keySet ());
    }
}