package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages;

import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SchemaElement(name = "deltix.Attribute")
public class Attribute {

    private AttributeId attributeId;
    private ExtendedAttribute extended;
    private ObjectArrayList<ExtendedAttribute> extendedAttributes;

    @SchemaElement
    public AttributeId getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(AttributeId attributeId) {
        this.attributeId = attributeId;
    }

    @SchemaElement
    public ExtendedAttribute getExtended() {
        return extended;
    }

    public void setExtended(ExtendedAttribute extended) {
        this.extended = extended;
    }

    @SchemaElement
    public ObjectArrayList<ExtendedAttribute> getExtendedAttributes() {
        return extendedAttributes;
    }

    public void setExtendedAttributes(ObjectArrayList<ExtendedAttribute> extendedAttributes) {
        this.extendedAttributes = extendedAttributes;
    }
}
