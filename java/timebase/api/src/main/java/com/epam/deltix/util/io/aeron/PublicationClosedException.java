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
package com.epam.deltix.util.io.aeron;

/**
 * This exception indicates that {@link deltix.data.stream.MessageChannel} (or other similar data receiver)
 * was closed and can't longer be used. Any further attempts to use this channel will fail.
 *
 * If you need to continue operation then you should re-create the channel.
 *
 * @author Alexei Osipov
 */
public class PublicationClosedException extends RuntimeException {
}