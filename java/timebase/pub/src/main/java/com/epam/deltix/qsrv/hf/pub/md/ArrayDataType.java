package com.epam.deltix.qsrv.hf.pub.md;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 */
@XmlType(name = "array")
public class ArrayDataType extends DataType {

    @XmlElement(name = "type")
    private DataType dataType;

    ArrayDataType () {
        // For deserialization
    }

    public ArrayDataType(boolean nullable, DataType dataType) {
        super(null, nullable);
        this.dataType = dataType;
    }

    public DataType getElementDataType() {
        return dataType;
    }

    @Override
    public ConversionType isConvertible(DataType to) {

        if (to instanceof ArrayDataType)
            return dataType.isConvertible(((ArrayDataType)to).dataType);

        return (ConversionType.NotConvertible);
    }

    @Override
    protected Object toBoxedImpl(CharSequence text) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    protected String toStringImpl(Object obj) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    protected void assertValidImpl(Object obj) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    public String   getBaseName() {
        return ("ARRAY");
    }

    @Override
    public int      getCode() {
        return T_ARRAY_TYPE;
    }

    @Override
    public String toString() {
        return ("ArrayDataType: " + dataType);
    }

    @Override
    public void writeTo(DataOutputStream out) throws IOException {
        out.writeByte(T_ARRAY_TYPE);
        super.writeTo (out);

        dataType.writeTo(out);
    }

    @Override
    protected void readFields(DataInputStream in, ClassDescriptor.TypeResolver resolver) throws IOException {
        super.readFields (in, resolver);

        dataType = DataType.readFrom (in, resolver);
    }
}
