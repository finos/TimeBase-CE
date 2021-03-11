package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.util.collections.Visitor;
import com.epam.deltix.util.io.UncheckedIOException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;

/**
 * Class representing message definition.
 */
@XmlRootElement(name = "recordClass")
@XmlType(name = "recordClass")
public final class RecordClassDescriptor
        extends ExtendableClassDescriptor<RecordClassDescriptor> {

    private static final long serialVersionUID = 1L;
    static final DataField[] NO_FIELDS = {};

    public static String printNames(RecordClassDescriptor... rcds) {
        StringBuilder sb = new StringBuilder("[");

        for (RecordClassDescriptor rcd : rcds) {
            sb.append(" ");
            sb.append(rcd.getName());
        }

        sb.append(" ]");
        return (sb.toString());
    }
    @XmlElement(name = "field")
    private DataField[] fields;

    public RecordClassDescriptor(
            String name,
            String title,
            boolean isAbstract,
            RecordClassDescriptor parent,
            DataField ... fields) {
        super(name, title, isAbstract, parent);
        this.fields = fields;
    }

    public RecordClassDescriptor(
            String guid,
            String name,
            String title,
            boolean isAbstract,
            RecordClassDescriptor parent,
            DataField... fields) {
        super(name, title, isAbstract, parent);
        this.guid = guid;
        this.fields = fields;
    }

    public RecordClassDescriptor(
            Class<?> inClass,
            RecordClassDescriptor inParent,
            DataField[] inFields) {
        this(inClass, ClassAnnotator.DEFAULT, inParent, inFields);
    }

    RecordClassDescriptor() {    // For JAXB
        fields = NO_FIELDS;
        guid = null;
    }

    RecordClassDescriptor(
            Class<?> inClass,
            ClassAnnotator annotator,
            RecordClassDescriptor inParent,
            DataField[] inFields) {

        super(inClass, annotator, inParent);

        assert guid != null;

        this.fields = inFields == null ? NO_FIELDS : inFields;
    }

    public RecordClassDescriptor(
            RecordClassDescriptor from,
            String[] excludingFields) {
        super(from);

        fields = new DataField[from.fields.length];
        System.arraycopy(from.fields, 0, fields, 0, fields.length);
        
        excludeFields(excludingFields);
    }

    public void excludeFields(String[] excludingFields) {
        List<DataField> resultFields = new ArrayList<DataField>();
        for (DataField df : fields) {
            boolean skipField = false;
            for (String f : excludingFields) {
                if (df.getName().equals(f)) {
                    skipField = true;
                    break;
                }
            }
            if (!skipField) {
                resultFields.add(df);
            }
        }
        fields = resultFields.toArray(new DataField[resultFields.size()]);
        if (getParent() != null) {
            getParent().excludeFields(excludingFields);
        }
    }

    void            changeFields(DataField[] fields) {
        // used by Instrospector only

        if (this.fields == NO_FIELDS)
            this.fields = fields;
        else
            throw new IllegalStateException("Fields collection already initialized");

    }

    public boolean  equals(RecordClassDescriptor rcd) {
        return this == rcd || (guid != null && guid.equals(rcd.guid));
    }

    @Override
    public boolean isEquals(ClassDescriptor target) {
        if (target instanceof RecordClassDescriptor && super.isEquals(target)) {
            RecordClassDescriptor rcd = (RecordClassDescriptor) target;

            if (rcd.fields.length != fields.length) {
                return false;
            }

            for (int i = 0; i < fields.length; i++) {
                if (!fields[i].isEquals(rcd.fields[i])) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public boolean fieldsEquals(ClassDescriptor target) {
        if (target instanceof RecordClassDescriptor) {
            RecordClassDescriptor rcd = (RecordClassDescriptor) target;

            if (rcd.fields.length != fields.length) {
                return false;
            }

            for (int i = 0; i < fields.length; i++) {
                if (!fields[i].isEquals(rcd.fields[i])) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public boolean hasField(String fname) {
        if (getParent() != null && getParent().hasField(fname)) {
            return (true);
        }

        for (DataField df : fields) {
            if (df.getName().equals(fname)) {
                return (true);
            }
        }

        return (false);
    }

    public DataField getField(String fname) {
        if (getParent() != null) {
            DataField df = getParent().getField(fname);

            if (df != null) {
                return (df);
            }
        }

        for (DataField df : fields) {
            if (df.getName().equals(fname)) {
                return (df);
            }
        }

        return (null);
    }

    public boolean hasFields() {
        return (fields.length > 0 || getParent() != null && getParent().hasFields());
    }

    public boolean isConvertibleTo(String className) {
        return (className.equals(getName())
                || getParent() != null && getParent().isConvertibleTo(className));
    }

    public boolean isAssignableFrom(RecordClassDescriptor c) {
        return (c.equals(this) || c.getParent() != null && isAssignableFrom(c.getParent()));
    }

    public DataField[] getFields() {
        return fields;
    }

    @Override
    public boolean visitDependencies(Visitor<ClassDescriptor> out) {
        if (!super.visitDependencies(out)) {
            return (false);
        }

        if (fields != null) {
            for (DataField f : fields) {
                final DataType ftype = f.getType();
                if (!visitDataType(out, ftype))
                    return false;
            }
        }

        return (true);
    }

    private static boolean visitDataType(Visitor<ClassDescriptor> out, DataType type) {
        if (type instanceof EnumDataType) {
            final EnumDataType etype = (EnumDataType) type;
            if (!out.visit(etype.descriptor)) {
                return (false);
            }
        } else if (type instanceof ClassDataType) {
            if(!visitClassDataType(out, (ClassDataType) type))
                return false;
        } else if (type instanceof ArrayDataType) {
            DataType atype = ((ArrayDataType) type).getElementDataType();
            if (!visitDataType(out, atype))
                return false;
        }

        return true;
    }

    private static boolean visitClassDataType(Visitor<ClassDescriptor> out, ClassDataType dataType) {
        final RecordClassDescriptor[] rcds = dataType.getDescriptors();

        if (rcds != null) {
            for (RecordClassDescriptor rcd : rcds) {
                if (!out.visit(rcd)) {
                    return (false);
                }
            }
        }

        return true;
    }

    public void dump(OutputStream os) {
        try {
            UHFJAXBContext.createMarshaller().marshal(this, os);
        } catch (JAXBException ex) {
            throw new UncheckedIOException(ex);
        }
    }

//    public void verifyGuid() {
//        if (guid == null) {
//            guid = createGuid();
//        }
//    }

    @Override
    public String toString() {
        return getName() + " #" + getGuid();
    }

    @Override
    public void writeTo(DataOutputStream out, int serial)
            throws IOException {
        out.writeByte(T_RECORD);

        super.writeTo(out, serial);

        int numFields = fields == null ? 0 : fields.length;
        out.writeShort(numFields);
        for (int ii = 0; ii < numFields; ii++)
            fields[ii].writeTo(out, serial);
    }

    @Override
    protected void readFields(
            DataInputStream     in,
            TypeResolver        resolver,
            int                 serial) throws IOException
    {

        super.readFields(in, resolver, serial);

        int numFields = in.readUnsignedShort();

        fields = new DataField[numFields];

        for (int ii = 0; ii < numFields; ii++) {
            fields[ii] = DataField.readFrom(in, resolver, serial);
        }
    }
}
