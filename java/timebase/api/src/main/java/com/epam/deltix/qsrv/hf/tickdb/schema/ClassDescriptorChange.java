package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.DataField;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.ArrayList;

public class ClassDescriptorChange implements SchemaChange {

    private String sourceId;
    private String targetId;

    @XmlElement
    protected Impact defaultImpact;
    
    @XmlElement
    protected AbstractFieldChange[] fieldChanges;
    
    ClassDescriptor source;
    ClassDescriptor target;

    protected ClassDescriptorChange() { } // for jaxb

    public ClassDescriptorChange(ClassDescriptor source, ClassDescriptor target, Impact defaultImpact) {
        this(source, target);
        this.defaultImpact = defaultImpact;
    }

    public ClassDescriptorChange(ClassDescriptor source, ClassDescriptor target) {
        this(source, target, new AbstractFieldChange[0]);
    }

    public ClassDescriptorChange(ClassDescriptor source,
                                 ClassDescriptor target,
                                 AbstractFieldChange[] fieldChanges) {
        this.fieldChanges = fieldChanges;
        this.source = source;
        this.sourceId = source != null ? source.getGuid() : null;
        this.target = target;
        this.targetId = target != null ? target.getGuid() : null;
    }

    public AbstractFieldChange[] getChanges() {
        return fieldChanges;
    }

//    public void setFieldChanges(AbstractFieldChange[] fieldChanges) {
//        this.fieldChanges = fieldChanges;
//    }

    public void setImpact(Impact impact) {
        this.defaultImpact = impact;
    }

    public Impact getChangeImpact() {
        if (defaultImpact != null)
            return defaultImpact;
        
         // modify
        if (getSource() != null && getTarget() != null) {
            Impact result = Impact.None;

            for (AbstractFieldChange change : getChanges()) {
               Impact imp = change.getChangeImpact();
               if (imp == Impact.DataLoss)
                   return Impact.DataLoss;
               else if (imp == Impact.DataConvert)
                   result = Impact.DataConvert;
            }
            return result;

        } else if (getTarget() != null) // create
            return Impact.None;
        else // delete
            return Impact.DataConvert;
    }

    public Impact getChangeImpact(SchemaMapping mapping) {
        if (defaultImpact != null)
            return defaultImpact;

        // modify
        if (getSource() != null && getTarget() != null) {
            Impact result = Impact.None;

            for (AbstractFieldChange change : getChanges()) {
                Impact imp;
                if (change instanceof EnumFieldTypeChange) {
                    imp = ((EnumFieldTypeChange) change).getChangeImpact(mapping);
                } else {
                    imp = change.getChangeImpact();
                }
                if (imp == Impact.DataLoss)
                    return Impact.DataLoss;
                else if (imp == Impact.DataConvert)
                    result = Impact.DataConvert;
            }
            return result;

        } else if (getTarget() != null) // create
            return Impact.None;
        else // delete
            return Impact.DataConvert;
    }

    public static Impact getChangeImpact(List<AbstractFieldChange> changes) {
        Impact result = Impact.None;

        for (AbstractFieldChange change : changes) {
            Impact imp = change.getChangeImpact();
            if (imp == Impact.DataLoss)
                return Impact.DataLoss;
            else if (imp == Impact.DataConvert)
                result = Impact.DataConvert;
        }

        return result;
    }

    public AbstractFieldChange[] getFieldChanges(DataField source, DataField target) {
        List<AbstractFieldChange> changes = new ArrayList<AbstractFieldChange>();
        
        for (int i = 0; fieldChanges != null && i < fieldChanges.length; i++) {
            AbstractFieldChange c = fieldChanges[i];
            if (source != null && SchemaAnalyzer.isEquals(source, c.getSource()))
                changes.add(c);
            else if (target != null && SchemaAnalyzer.isEquals(target, c.getTarget()))
                changes.add(c);
        }
        
        return changes.toArray(new AbstractFieldChange[changes.size()]);
    }  

    @XmlElement
    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    @XmlElement
    public String getTargetId() {
        return targetId;
    }
    
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public ClassDescriptor getSource() {
        return source;
    }

    public ClassDescriptor getTarget() {
        return target;
    }
}
