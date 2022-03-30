package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.util.Arrays;
import java.util.Collections;

/**
 * Helper class
 */
public class Builder {
    public static NonStaticField createNonStatic(String title, String name, FieldType type) {
        NonStaticField field = new NonStaticField();
        field.setTitle(title);
        field.setName(name);
        field.setType(type);
        field.setIsPrimaryKey(false);

        return field;
    }

    public static TypeDescriptor createDescriptor(String title, String name, ObjectArrayList<Field> fields) {
        TypeDescriptor descriptor = new TypeDescriptor();

        descriptor.setTitle(title);
        descriptor.setName(name);
        descriptor.setFields(fields);
        descriptor.setIsAbstract(false);
        descriptor.setIsContentClass(false);

        return descriptor;
    }

    public static TypeDescriptor createDescriptor(String title, String name, Field ... fields) {
        TypeDescriptor descriptor = new TypeDescriptor();

        descriptor.setTitle(title);
        descriptor.setName(name);
        descriptor.setFields(new ObjectArrayList<>(fields));
        descriptor.setIsAbstract(false);
        descriptor.setIsContentClass(false);

        return descriptor;
    }

}
