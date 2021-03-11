package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.tool.gson.CharSequenceTypeAdapter;
import com.epam.deltix.util.s3.S3DataStore;

import java.io.IOException;
import java.util.Set;

import static com.epam.deltix.util.s3.S3DataStore.KEY_DELIMITER;

public class S3Utils {

    public static S3Utils INSTANCE = new S3Utils();

    private S3Utils() {
    }

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(CharSequence.class, new CharSequenceTypeAdapter())
            .create();

    public S3SpaceMetadata parseMetadata(String metadata) {
        return gson.fromJson(metadata, S3SpaceMetadata.class);
    }

    public String serializeMetadata(S3SpaceMetadata metadata) {
        return gson.toJson(metadata);
    }

    public String serializeMetadata(DXTickStream stream, String space, long firstTimestamp) {
        return serializeMetadata(S3SpaceMetadata.fromStream(stream, space, firstTimestamp));
    }

    public static String getDataKey(DXTickStream stream, String spaceId) {
        String dataKey = stream.getKey();
        if (spaceId != null) {
            dataKey += KEY_DELIMITER + "space=" + spaceId;
        }
        return dataKey;
    }

    public static String[] getSpaces(S3DataStore dataStore, String streamKey) {
        Set<String> partitions = dataStore.getPartitions(streamKey, "");
        String[] spaces = new String[partitions.size()];
        int i = 0;
        for (String partition : partitions) {
            spaces[i++] = getSpaceFromDataKey(partition);
        }
        return spaces;
    }

    public static String getSpaceFromDataKey(String dataKey) {
        int index = dataKey.lastIndexOf("=");
        return dataKey.substring(index + 1);
    }

    public static long getFirstMessageTimestamp(
            S3DataStore dataStore,
            String dataKey,
            long startTime, long endTime,
            RecordClassDescriptor[] types
    ) throws IOException {
        try (S3RawMessageReader reader = new S3RawMessageReader(dataStore, dataKey, startTime, endTime, types)) {
            RawMessage raw = reader.read();
            return raw == null ? Long.MIN_VALUE : raw.getTimeStampMs();
        }
    }

    public static long getMinTimestamp(
            S3DataStore dataStore,
            String[] dataKeys,
            long startTime, long endTime,
            RecordClassDescriptor[] types
    ) throws IOException {
        long result = Long.MAX_VALUE;
        for (String dataKey : dataKeys) {
            long timestamp = getFirstMessageTimestamp(dataStore, dataKey, startTime, endTime, types);
            if (timestamp < result) {
                result = timestamp;
            }
        }
        return result;
    }

}
