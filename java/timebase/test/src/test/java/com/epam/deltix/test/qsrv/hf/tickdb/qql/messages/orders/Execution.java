package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.Attribute;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.CustomAttribute;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.FixAttribute;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.ObjectArrayList;


@SchemaElement(name = "deltix.orders.Execution")
public class Execution {

    private Id id;
    private ExecutionInfo info;
    private ObjectArrayList<Attribute> attributes = new ObjectArrayList<>();
    private ObjectArrayList<CharSequence> customTags = new ObjectArrayList<>();

    @SchemaElement(title = "id")
    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    @SchemaElement(title = "info")
    public ExecutionInfo getInfo() {
        return info;
    }

    public void setInfo(ExecutionInfo info) {
        this.info = info;
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

    @SchemaElement
    public ObjectArrayList<CharSequence> getCustomTags() {
        return customTags;
    }

    public void setCustomTags(ObjectArrayList<CharSequence> customTags) {
        this.customTags = customTags;
    }

    @Override
    public String toString() {
        return "Execution{" +
            "id=" + id +
            '}';
    }
}
