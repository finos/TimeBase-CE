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

    public ClassDescriptorArray(RecordClassDescriptor concrete, RecordClassSet sentRCS) {
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
                if (sentRCS.getClassDescriptor(cd.getName()) == null)
                    list.add(cd);
            }
        }

        descriptors = list.toArray(new ClassDescriptor[list.size()]);
    }

    public ClassDescriptor[] getDescriptors() {
        return descriptors;
    }
}
