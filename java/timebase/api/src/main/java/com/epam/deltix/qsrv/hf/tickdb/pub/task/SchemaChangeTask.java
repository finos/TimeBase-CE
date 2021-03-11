package com.epam.deltix.qsrv.hf.tickdb.pub.task;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

@XmlRootElement(name="SchemaChangeTask")
public class SchemaChangeTask implements TransformationTask {

    @XmlElement
    public boolean background = true;

    @XmlElement
    public StreamMetaDataChange change;

    public SchemaChangeTask() { } // 4JAXB

    public SchemaChangeTask(StreamMetaDataChange change) {
        this.change = change;
    }

    public SchemaChangeTask(StreamMetaDataChange change, boolean background) {
        this.change = change;
        this.background = background;
    }

    @Override
    public boolean              isBackground() {
        return background;
    }

    public void                 setBackground(boolean value) {
        background = value;
    }

    public void                 invalidate(RecordClassSet set) {
        change.invalidate(set);
    }
}
