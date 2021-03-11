package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import java.util.*;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler.*;

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
        RecordClassInfo         ci = (RecordClassInfo) infoMap.get (rcd);

        if (ci != null)
            return (ci);

        RecordClassDescriptor   parentRCD = rcd.getParent ();
        RecordClassInfo         pci = parentRCD == null ? null : registerClass (parentRCD);

        ci = new RecordClassInfo (pci, rcd);

        if (pci != null)
            pci.directSubclasses.add (ci);

        typeEnv.bind (NamedObjectType.TYPE, rcd.getName (), ci);
        infoMap.put (rcd, ci);

        return (ci);
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
