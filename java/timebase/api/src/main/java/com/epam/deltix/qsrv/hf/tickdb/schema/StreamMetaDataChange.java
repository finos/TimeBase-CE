package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.lang.Depends;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;

@XmlType(name="streamMetaDataChange")
@XmlRootElement(name="StreamMetaDataChange")
@Depends("../jaxb.index")
public class StreamMetaDataChange extends MetaDataChange {

    public StreamMetaDataChange() { } // for jaxb

    public StreamMetaDataChange(ClassSet<RecordClassDescriptor> source,
                                MetaData<RecordClassDescriptor> target,
                                SchemaMapping mapping) {
        super(source, target, mapping);
    }

    @XmlElement()
    public RecordClassSet       getMetaData() {
        return (RecordClassSet)target;
    }

    public void                 setMetaData(RecordClassSet target) {
        this.target = target;
    }

    @XmlElement()
    public RecordClassSet       getSource() {
        return (RecordClassSet)source;
    }

    public void                 setSource(RecordClassSet source) {
        this.source = source;
    }

    public void                 invalidate(RecordClassSet source) {
        this.source = source;

        // when both source & target have content classes with same guid JAXB may mess up instances
        ((RecordClassSet)target).fix();

        for (ClassDescriptorChange change : changes) {
            if (change.getSource() == null && change.getSourceId() != null)
                change.source = source.findClass(change.getSourceId());

            if (change.getTarget() == null && change.getTargetId() != null)
                change.target = getMetaData().findClass(change.getTargetId());
        }
    }

}
