package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedUnboundEncoder;

class ClassDescriptorMapping {
    public RecordClassDescriptor        target;
    public RecordClassDescriptor        source;

    public final FieldMapping[]         mappings;

    public UnboundDecoder               decoder;
    public FixedUnboundEncoder          encoder;

    ClassDescriptorMapping(RecordClassDescriptor source,
                           RecordClassDescriptor target,
                           SchemaMapping schemaMapping)
    {
        this.source = source;
        this.target = target;
        this.encoder = CodecFactory.INTERPRETED.createFixedUnboundEncoder(target);
        this.decoder = CodecFactory.COMPILED.createFixedUnboundDecoder(source);

        this.mappings = schemaMapping.getMappings(source, target);
    }

    ClassDescriptorMapping(ClassDescriptorChange change, SchemaMapping schemaMapping) {
        this(   (RecordClassDescriptor)change.getSource(),
                (RecordClassDescriptor)change.getTarget(),
                schemaMapping
        );

        for (FieldMapping mapping : mappings) {
            AbstractFieldChange[] fieldChanges = change.getFieldChanges(
                    mapping.source != null ? mapping.source.getField() : null,
                    mapping.target != null ? mapping.target.getField() : null);

            // assuming we ca have only 1 change
            for (AbstractFieldChange fieldChange : fieldChanges)
                mapping.resolution = fieldChange.resolution;
        }
    }
}
