package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.lang.Depends;
import javax.xml.bind.annotation.*;


@XmlType(name="copyStreamMetaDataChange")
@XmlRootElement(name="CopyStreamMetaDataChange")
@Depends("../jaxb.index")
public class CopyStreamMetaDataChange extends MetaDataChange {

    public CopyStreamMetaDataChange() { } // for jaxb

    public CopyStreamMetaDataChange(ClassSet<RecordClassDescriptor> source,
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
    public SimpleClassSet       getSource() {
        return (SimpleClassSet)source;
    }

    public void                 setSource(SimpleClassSet source) {
        this.source = source;
    }

}
