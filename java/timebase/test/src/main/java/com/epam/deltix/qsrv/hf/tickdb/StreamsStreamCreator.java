/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
