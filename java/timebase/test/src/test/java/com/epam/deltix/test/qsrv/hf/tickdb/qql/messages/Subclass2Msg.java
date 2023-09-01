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

package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "Subclass2")
public class Subclass2Msg extends SubclassMsg {

    private double sss;

    private double sss2;

    public Subclass2Msg() {
    }

    public Subclass2Msg(double sss0, double sss, double sss2) {
        super(sss0);
        this.sss = sss;
        this.sss2 = sss2;
    }

    @SchemaElement(title = "sss")
    public double getSss() {
        return sss;
    }

    public void setSss(double sss) {
        this.sss = sss;
    }

    @SchemaElement(title = "sss")
    public double getSss2() {
        return sss2;
    }

    public void setSss2(double sss2) {
        this.sss2 = sss2;
    }
}
