package com.epam.deltix.qsrv.solgen;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StreamMetaData {
    private String nameSpace;
    private Set<RecordClassDescriptor> allTypes = new HashSet<>();
    private List<RecordClassDescriptor> concreteTypes = new ArrayList<>();

    public StreamMetaData(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public boolean addType(RecordClassDescriptor descriptor) {
        return allTypes.add(descriptor);
    }

    public boolean addConcreteType(RecordClassDescriptor descriptor) {
        return concreteTypes.add(descriptor);
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public Set<RecordClassDescriptor> getAllTypes() {
        return allTypes;
    }

    public List<RecordClassDescriptor> getConcreteTypes() {
        return concreteTypes;
    }
}
