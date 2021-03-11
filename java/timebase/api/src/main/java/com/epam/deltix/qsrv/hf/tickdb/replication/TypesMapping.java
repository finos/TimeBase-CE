package com.epam.deltix.qsrv.hf.tickdb.replication;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.collections.SmallArrays;

/**
 *
 */
class TypesMapping {

    public RecordClassDescriptor[] target;
    public RecordClassDescriptor[] types;

    public TypesMapping(RecordClassDescriptor[] types, RecordClassDescriptor[] target) {
        this.types = types;
        this.target = target;        
    }

    public RecordClassDescriptor    getType(RecordClassDescriptor source) {
        int index = SmallArrays.indexOf(source, types);
        assert index != -1;
        return target[index];
    }
}
