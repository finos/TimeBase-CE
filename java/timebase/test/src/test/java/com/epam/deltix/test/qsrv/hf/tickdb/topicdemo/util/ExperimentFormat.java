/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util;

import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.EchoMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.MessageWithNanoTime;

/**
 * @author Alexei Osipov
 */
public enum ExperimentFormat {
    ONE_TOPIC(false, MessageWithNanoTime.class),
    TWO_TOPIC_TWO_MESSAGES(true, EchoMessage.class),
    TWO_TOPIC_ONE_MESSAGE(true, MessageWithNanoTime.class);

    private final boolean echoTopic;
    private final Class echoMessageClass;

    ExperimentFormat(boolean echoTopic, Class echoMessageClass) {
        this.echoTopic = echoTopic;
        this.echoMessageClass = echoMessageClass;
    }

    public boolean useMainChannel() {
        return !echoTopic;
    }

    public boolean useEchoChannel() {
        return echoTopic;
    }

    public Class getEchoMessageClass() {
        return echoMessageClass;
    }
}
