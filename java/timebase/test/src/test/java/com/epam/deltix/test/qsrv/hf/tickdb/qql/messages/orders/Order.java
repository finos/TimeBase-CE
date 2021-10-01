package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.Attribute;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.CustomAttribute;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.FixAttribute;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.SchemaArrayType;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SchemaElement(name = "deltix.orders.Order")
public class Order extends InstrumentMessage {

    private float sequence;
    protected Id id;
    private ObjectArrayList<Attribute> attributes = new ObjectArrayList<>();

    @SchemaElement(title = "sequence")
    public float getSequence() {
        return sequence;
    }

    public void setSequence(float sequence) {
        this.sequence = sequence;
    }

    @SchemaElement(title = "id")
    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    @SchemaElement(title = "attributes")
    @SchemaArrayType(
        isNullable = false,
        isElementNullable = false,
        elementTypes =  {
            CustomAttribute.class, FixAttribute.class
        }
    )
    public ObjectArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(ObjectArrayList<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "Order{" +
            "sequence=" + sequence +
            ", id=" + id +
            '}';
    }

}
