package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

/**
 * @author Alexei Osipov
 */
public class GetTopicInstrumentMappingRequest extends BaseTopicRequest {
    private final int dataStreamId; // We send data streamId so we can ensure that snapshot is for the exactly same topic

    public GetTopicInstrumentMappingRequest(String topicKey, int dataStreamId) {
        super(topicKey);
        this.dataStreamId = dataStreamId;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }
}
