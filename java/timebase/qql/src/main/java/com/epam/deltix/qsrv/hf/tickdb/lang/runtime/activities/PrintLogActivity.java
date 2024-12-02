/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.activities;

import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.timebase.messages.service.ErrorLevel;

/**
 * 
 */
public class PrintLogActivity extends LoggingActivityLauncher {
    private final String type;
    private final ErrorLevel level;
    private final String messageText;
    private final String details;

    public PrintLogActivity(String type, ErrorLevel level, String messageText, String details) {
        this.type = type;
        this.level = level;
        this.messageText = messageText;
        this.details = details;
    }

    @Override
    protected LoggingActivity createActivity() {
        return (
            new LoggingActivity() {
                @Override
                protected void run(ReadableValue[] params) {
                    log(type, level, messageText, details);
                }
            }
        );
    }

    @Override
    public ClassSet<RecordClassDescriptor> getSchema() {
        ClassSet<RecordClassDescriptor> set = new RecordClassSet();
        set.addContentClasses(Messages.ERROR_MESSAGE_DESCRIPTOR);
        return set;
    }
}
