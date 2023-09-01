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
package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.testmode;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.ExperimentFormat;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.*;

/**
 * @author Alexei Osipov
 */
public enum CommunicationType implements TestComponentProvider {
    TOPIC {
        @Override
        public ReadAndReplyBase getReader() {
            return new ReadAndReplyTopic();
        }

        @Override
        public WriteBase getWriter() {
            return new WriteTopic();
        }

        @Override
        public ReadEchoBase getEchoReader(ExperimentFormat experimentFormat) {
            return new ReadEchoTopic(experimentFormat);
        }
    },

    SOCKET_STREAM {
        @Override
        public ReadAndReplyBase getReader() {
            return new ReadAndReplyStream(ChannelPerformance.LOW_LATENCY);
        }

        @Override
        public WriteBase getWriter() {
            return new WriteStream(ChannelPerformance.LOW_LATENCY);
        }

        @Override
        public ReadEchoBase getEchoReader(ExperimentFormat experimentFormat) {
            return new ReadEchoStream(ChannelPerformance.LOW_LATENCY, experimentFormat);
        }
    },

    IPC_STREAM {
        @Override
        public ReadAndReplyBase getReader() {
            return new ReadAndReplyStream(ChannelPerformance.LATENCY_CRITICAL);
        }

        @Override
        public WriteBase getWriter() {
            return new WriteStream(ChannelPerformance.LATENCY_CRITICAL);
        }

        @Override
        public ReadEchoBase getEchoReader(ExperimentFormat experimentFormat) {
            return new ReadEchoStream(ChannelPerformance.LATENCY_CRITICAL, experimentFormat);
        }
    }
}