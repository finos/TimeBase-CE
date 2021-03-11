package com.epam.deltix.qsrv.util.json.parser;

import com.epam.deltix.qsrv.hf.pub.md.NamedDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class NoSuchDescriptorInSchemaException extends Exception {
    public NoSuchDescriptorInSchemaException(String lost, RecordClassDescriptor[] rcds) {
        super(format("Schema does not contain descriptor %s, list of descriptors: %s.",
                lost,
                Arrays.stream(rcds)
                        .map(NamedDescriptor::getName)
                        .collect(Collectors.joining(", "))));
    }
}
