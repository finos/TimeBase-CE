package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

/**
 * @author Alexei Osipov
 */
public class GetTopicTemporaryInstrumentMappingRequest extends BaseTopicRequest {
    private final int dataStreamId; // We send data streamId so we can ensure that snapshot is for the exactly same topic
    private final int requestedTempEntityIndex; // Temp entry index that client wants to look up

    public GetTopicTemporaryInstrumentMappingRequest(String topicKey, int dataStreamId, int requestedTempEntityIndex) {
        super(topicKey);
        this.dataStreamId = dataStreamId;
        this.requestedTempEntityIndex = requestedTempEntityIndex;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }

    public int getRequestedTempEntityIndex() {
        return requestedTempEntityIndex;
    }
}
