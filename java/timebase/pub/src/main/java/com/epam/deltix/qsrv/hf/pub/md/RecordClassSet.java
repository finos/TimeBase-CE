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

import static com.epam.deltix.qsrv.hf.pub.md.ClassDescriptorSearchOptions.INCLUDE_ABSTRACT_RECORD;
import static com.epam.deltix.qsrv.hf.pub.md.ClassDescriptorSearchOptions.INCLUDE_CONCRETE_RECORD;
import static com.epam.deltix.qsrv.hf.pub.md.ClassDescriptorSearchOptions.INCLUDE_ENUM;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.epam.deltix.util.collections.Visitor;
import com.epam.deltix.util.lang.StringUtils;

import javax.xml.bind.annotation.XmlIDREF;

/**
 *  Finds all dependent classes.
 */
@XmlRootElement (name = "classSet")
public class RecordClassSet implements MetaData<RecordClassDescriptor>, Serializable {
    private static final long                       serialVersionUID = 1L;
	private static final Object                     AMBIGUOUS = "<AMBIGUOUS>";
    private static final ClassDescriptor []         NO_CDS = { };
    
    private transient Map <String, Object>          nameToClassMap;
            
    @XmlElement (name = "classDescriptor")
    private HashSet <ClassDescriptor>               classDescriptors;

    @XmlElement (name = "contentClassId") @XmlIDREF
    private List <RecordClassDescriptor>            contentClasses = null;

    private HashSet <Runnable>                      changeListeners = null;

    /**
     *  Used by JAXB
     */
    public RecordClassSet () {
    }
    
    public RecordClassSet (RecordClassSet copy) {
        set (copy);
    }
    
    public RecordClassSet (RecordClassDescriptor [] topTypes) {
        addContentClasses (topTypes);
    }

    public synchronized void        addChangeListener (Runnable l) {
        if (changeListeners == null)
            changeListeners = new HashSet <Runnable> ();

        changeListeners.add (l);
    }

    public synchronized void        removeChangeListener (Runnable l) {
        if (changeListeners != null)
            changeListeners.remove (l);
    }

    public synchronized int                      getNumTopTypes () {
        return (contentClasses == null ? 0 : contentClasses.size ());
    }
    
    public synchronized RecordClassDescriptor    getTopType (int idx) {
        return (contentClasses.get (idx));
    }
    
    public synchronized RecordClassDescriptor []      getTopTypes () {
        final int                       num = getNumTopTypes ();
        final RecordClassDescriptor []  ret = new RecordClassDescriptor [num];
        
        for (int ii = 0; ii < num; ii++)
            ret [ii] = getTopType (ii);
        
        return (ret);
    }

    private static final Runnable []    EMPTY = { };
    
    private Runnable []     snapListeners () {
        assert Thread.holdsLock (this);
        
        return (
            changeListeners == null ?
                EMPTY :
                changeListeners.toArray (new Runnable [changeListeners.size ()])
        );
    }

    private void            fireListeners (Runnable [] snapshot) {
        assert !Thread.holdsLock (this);

        for (Runnable r : snapshot)
            r.run ();
    }

    private void            clear2 () {        

        if (contentClasses != null)
            contentClasses.clear ();

        if (classDescriptors != null)
            classDescriptors.clear ();

        if (nameToClassMap != null)
            nameToClassMap.clear ();
    }

    public void             clear () {
        Runnable []     listeners;

        synchronized (this) {
            clear2 ();

            listeners = snapListeners ();
        }

        fireListeners (listeners);
    }

    public synchronized RecordClassDescriptor   getContentClass(String guid) {
        final int                       num = getNumTopTypes ();

        for (int i = 0; i < num; i++) {           
            RecordClassDescriptor descriptor = getTopType(i);
            if (descriptor.getGuid().equals(guid))
                return descriptor;
        }

        return null;
    }

    public synchronized ClassDescriptor         findClass(String guid) {
        if (classDescriptors != null) {
            Optional<ClassDescriptor> match = classDescriptors.stream().filter(x -> guid.equals(x.guid)).findFirst();
            return match.orElse(null);
        }
        
        return null;
    }

    @Override
    public synchronized ClassDescriptor[]            getClasses() {
        return classDescriptors != null ?
                classDescriptors.toArray(new ClassDescriptor[classDescriptors.size()]) :
                new ClassDescriptor[0];
    }

    public synchronized RecordClassDescriptor[] getContentClasses() {
        return getTopTypes();
    }

//    public boolean hasClass(String name) {
//        return nameToClassMap.containsKey(name);
//    }

    public  void                addClasses(ClassDescriptor ...  cds) {
        Runnable []     listeners;

        synchronized (this) {
            defineClass2 (cds);
            listeners = snapListeners ();
        }

        fireListeners (listeners);
    }

    public void                     addContentClasses (RecordClassDescriptor ...  cds)
        throws DuplicateClassNameException
    {
        Runnable []     listeners;

        synchronized (this) {
            defineClass2 (cds);

            if (contentClasses == null)
                contentClasses = new ArrayList <RecordClassDescriptor> ();

            contentClasses.addAll(Arrays.asList(cds));

            listeners = snapListeners ();
        }

        fireListeners (listeners);
    }

    public void                 set(final RecordClassSet set) {
        Runnable []     listeners;

        synchronized (this) {
            clear2 ();

            if (classDescriptors == null)
                classDescriptors = new HashSet <ClassDescriptor> ();

            if (contentClasses == null)
                contentClasses = new ArrayList <RecordClassDescriptor> ();

            set.copyClasses(this);
            
            buildNameIndex();

            listeners = snapListeners ();
        }

        fireListeners (listeners);
    }

    public void                 setClassDescriptors (ClassDescriptor ... cds) {
        Runnable []     listeners;

        synchronized (this) {
            clear2 ();
            defineClass2 (cds);

            listeners = snapListeners ();
        }

        fireListeners (listeners);
    }

    private void                 defineClass2 (ClassDescriptor [] cds) {
        final Queue <ClassDescriptor>           addQueue = 
            new ArrayDeque <ClassDescriptor> ();
                
        for (ClassDescriptor cd : cds)
            addQueue.offer (cd);

        final Visitor <ClassDescriptor>         adder = 
            new Visitor <ClassDescriptor> () {                
                public boolean                      visit (ClassDescriptor cd) {
                    addQueue.offer (cd);
                    return (true);
                }
            }; 
        
        buildNameIndex ();
            
        for (;;) {
            ClassDescriptor             cd = addQueue.poll ();
            
            if (cd == null)
                break;

            if (classDescriptors == null)
                classDescriptors = new HashSet <ClassDescriptor> ();

            if (!classDescriptors.add (cd))            
                continue;

            if (nameToClassMap == null)
                nameToClassMap = new HashMap <String, Object> ();

            addClassToNameIndex (cd);
            
            cd.visitDependencies (adder);
        }

        buildNameIndex ();
    }

    private void                                addClassToNameIndex (ClassDescriptor cd) {        
        String                      cdname = cd.getName ();
        Object                      dup = nameToClassMap.get (cdname);

        if (dup == null)
            nameToClassMap.put (cdname, cd);
        else if (dup != AMBIGUOUS)
            nameToClassMap.put (cdname, AMBIGUOUS);
    }

//    public MessageEncoder <RawMessage>          createRawEncoder () {
//        return (StreamConfigurationHelper.createRawEncoder (null, null, getTopTypes ()));
//    }
//
//    public MessageEncoder <InstrumentMessage>   createBoundEncoder (CodecFactory factory, TypeLoader loader) {
//        return (StreamConfigurationHelper.createBoundEncoder (factory, null, null, loader, getTopTypes ()));
//    }
//
//    public MessageDecoder <RawMessage>          createRawDecoder () {
//        return (StreamConfigurationHelper.createRawDecoder (null, null, getTopTypes ()));
//    }
//
//    public MessageDecoder <InstrumentMessage>   createBoundDecoder (CodecFactory factory, TypeLoader loader) {
//        return (StreamConfigurationHelper.createBoundDecoder (factory, null, null, loader, getTopTypes ()));
//    }
    
    private void                                buildNameIndex () {
        if (nameToClassMap == null && classDescriptors != null) {
            nameToClassMap = new HashMap <String, Object> (classDescriptors.size ());
            
            for (ClassDescriptor cd : classDescriptors)
                addClassToNameIndex (cd);
        }
    }
    
    public synchronized ClassDescriptor         getClassDescriptor (String name) {
        buildNameIndex ();
        
        if (nameToClassMap == null)
            return (null);
        
        Object      obj = nameToClassMap.get (name);

        if (obj == AMBIGUOUS)
            throw new IllegalStateException ("Ambiguous class name: " + name);

        return ((ClassDescriptor) obj);
    }

    public synchronized DataField         findField (String guid, String fieldName) {

        for (ClassDescriptor cd : classDescriptors) {
            if (StringUtils.equals(cd.getGuid(), guid)) {
                if (cd instanceof RecordClassDescriptor)
                    return ((RecordClassDescriptor) cd).getField(fieldName);
            }
        }

        return null;
    }
    
    public synchronized ClassDescriptor []      selectClassDescriptors (
        int                                         options, 
        String                                      namePattern
    )
    {
        if (classDescriptors == null)
            return (NO_CDS);

        Matcher                         m;
        
        if (namePattern == null)
            m = null;
        else
            m = Pattern.compile (namePattern).matcher ("");
        
        ArrayList <ClassDescriptor>     tmp = new ArrayList <ClassDescriptor> ();

        
        for (ClassDescriptor cd : classDescriptors) {
            if ((cd instanceof EnumClassDescriptor) && (options & INCLUDE_ENUM) == 0)
                continue;

            if (cd instanceof RecordClassDescriptor) {
                RecordClassDescriptor   rcd = (RecordClassDescriptor) cd;

                if (rcd.isAbstract () && (options & INCLUDE_ABSTRACT_RECORD) == 0)
                    continue;

                if (!rcd.isAbstract () && (options & INCLUDE_CONCRETE_RECORD) == 0)
                    continue;
            }

            if (m != null && !m.reset (cd.getName ()).matches ())
                continue;

            tmp.add (cd);
        }

        ClassDescriptor []      ret = new ClassDescriptor [tmp.size ()];

        tmp.toArray (ret);
        Arrays.sort (ret, ClassDescriptor.ASCENDING_COMPARATOR);

        return (ret);
    }

    protected synchronized void copyClasses(RecordClassSet set) {
        if (classDescriptors != null)
            set.classDescriptors.addAll(classDescriptors);

        if (contentClasses != null)
            set.contentClasses.addAll(contentClasses);
    }
    
    public synchronized ClassDescriptor []      getClassDescriptors () {
        if (classDescriptors == null)
            return (NO_CDS);        

        ClassDescriptor []      ret = new ClassDescriptor [classDescriptors.size ()];

        classDescriptors.toArray (ret);
        Arrays.sort (ret, ClassDescriptor.ASCENDING_COMPARATOR);

        return (ret);
    }

    public synchronized void         fix() {

        // replace class descriptors to make sure that contentClasses
        // contains instances from classDescriptors

        ArrayList<RecordClassDescriptor> changed = new ArrayList<RecordClassDescriptor>();
        for (RecordClassDescriptor rcd : contentClasses) {
            // search by guid
            for (ClassDescriptor c : classDescriptors)
                if (c.getGuid().equals(rcd.getGuid())) {
                    changed.add((RecordClassDescriptor)c);
                }
        }

        for (RecordClassDescriptor rcd : changed) {
            contentClasses.remove(rcd);
            contentClasses.add(rcd);
        }
    }

//    public void verifyGuids() {
//        for (ClassDescriptor classDescriptor : getClassDescriptors()) {
//            if (classDescriptor instanceof RecordClassDescriptor)
//                ((RecordClassDescriptor)classDescriptor).verifyGuid();
//        }
//
//    }
}