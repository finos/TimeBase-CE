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
package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "Subclass")
public class SubclassMsg extends InstrumentMessage {

    private double sss0;

    public SubclassMsg() {
    }

    public SubclassMsg(double sss0) {
        this.sss0 = sss0;
    }

    @SchemaElement(title = "sss")
    public double getSss0() {
        return sss0;
    }

    public void setSss0(double sss0) {
        this.sss0 = sss0;
    }
}
