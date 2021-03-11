package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import java.util.List;

/**
 * @author Alexei Osipov
 */
public class GetTopicMetadataResponse {
    private final List<RecordClassDescriptor> types;

    public GetTopicMetadataResponse(List<RecordClassDescriptor> types) {
        this.types = types;
    }

    public List<RecordClassDescriptor> getTypes() {
        return types;
    }
}
