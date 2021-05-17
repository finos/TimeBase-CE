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
package com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception;

/**
 * Thrown in case of attempt to access an IPC-only topic from remote machine.
 * Note: topic must be created as multicast topic if you want to access it from other machines.
 *
 * Note: this class does not implement {@link TopicApiException} because this exception is severe and it is not a part of API.
 *
 * @author Alexei Osipov
 */
public class RemoteAccessToLocalTopic extends RuntimeException {
    public RemoteAccessToLocalTopic() {
        super("Attempt to access topic on remote TB via IPC");
    }

    public RemoteAccessToLocalTopic(String message) {
        super(message);
    }
}
