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
package com.epam.deltix.util.io.idlestrat;

/**
 * Different implementation
 */
public interface IdleStrategy {
    /**
     * @param workCount amount of work done in the last cycle. Value "0" means that no work as done and some data form an external source expected.
     */
    void idle(int workCount);

    /**
     * Idle action (sleep, wait, etc).
     */
    void idle();

    /**
     * Reset the internal state (after doing some work).
     */
    void reset();
}