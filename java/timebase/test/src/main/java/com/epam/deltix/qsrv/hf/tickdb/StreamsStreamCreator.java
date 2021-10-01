package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import java.util.Arrays;
import java.util.Comparator;

@SuppressWarnings("rawtypes")
public class StreamsStreamCreator {

//    public static void main(String[] args) {
//        ObjectArrayList<StreamMessage> streamMessages = new ObjectArrayList<>();
//        try (DXTickDB db = TickDBFactory.openFromUrl("dxtick://localhost:8011", false)) {
//            InternalFunctions.streams(db, streamMessages);
//            DXTickStream stream = db.getStream("streams");
//            if (stream != null) {
//                stream.delete();
//            }
//            StreamOptions streamOptions = new StreamOptions(StreamScope.DURABLE, "streams", null, StreamOptions.MAX_DISTRIBUTION);
//            streamOptions.setPolymorphic(schema());
//            stream = db.createStream("streams", streamOptions);
//            Arrays.stream(stream.getAllDescriptors())
//                    .sorted(Comparator.comparing(ClassDescriptor::getGuid))
//                    .forEach(cd -> System.out.println(cd.getGuid() + ": " + cd.getName()));
//            LoadingOptions options = new LoadingOptions();
//            options.channelQOS = ChannelQualityOfService.MIN_INIT_TIME;
//            try (TickLoader loader = stream.createLoader(options)) {
//                streamMessages.forEach(loader::send);
//            }
//        }
//    }
//
//    private static RecordClassDescriptor schema() {
//        try {
//            return Introspector.createEmptyMessageIntrospector().introspectRecordClass(StreamMessage.class);
//        } catch (Introspector.IntrospectionException e) {
//            throw new RuntimeException(e);
//        }
//    }

}
