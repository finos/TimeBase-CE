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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.codec.cg.StringBuilderPool;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.InstancePool;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public abstract class FilterState {

    boolean accepted = false;
    boolean waitingByTime = false;
    int messagesCount = 0;
    boolean initializedOnInterval = false;
    boolean initialized = false;
    protected final StringBuilderPool varcharPool;
    protected final InstancePool instancePool;

    String warningMessage;

    private final FilterIMSImpl filter;

    public FilterState(FilterIMSImpl filter) {
        this.filter = filter;
        if (filter != null) {
            this.varcharPool = filter.varcharPool;
            this.instancePool = filter.instancePool;
        } else {
            this.varcharPool = null;
            this.instancePool = null;
        }
    }

    public boolean isAccepted() {
        return (accepted);
    }

    public void setWarningMessage(String message) {
        this.warningMessage = message;
    }

    protected RawMessage getLastMessage() {
        throw new UnsupportedOperationException();
    }

    protected MemoryDataOutput getOut() {
        throw new UnsupportedOperationException();
    }

    protected void resetFunctions() {
    }

    protected RecordClassDescriptor getDescriptor(String descriptorName) {
        ClassDescriptor cd = filter.inputClassSet.getClassDescriptor(descriptorName);
        if (cd instanceof RecordClassDescriptor) {
            return (RecordClassDescriptor) cd;
        }

        return null;
    }
}