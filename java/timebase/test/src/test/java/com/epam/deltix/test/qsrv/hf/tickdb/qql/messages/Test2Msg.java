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

import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;


@SchemaElement(name = "Test2")
public class Test2Msg extends InstrumentMessage {

    private double aaa;

    private SubclassMsg ooo;

    private ObjectArrayList<SubclassMsg> oooaaa;

    @SchemaElement(title = "aaa")
    public double getAaa() {
        return aaa;
    }

    public void setAaa(double aaa) {
        this.aaa = aaa;
    }

    @SchemaElement(title = "ooo")
    @SchemaType(
        dataType = SchemaDataType.OBJECT,
        nestedTypes = {
            Subclass2Msg.class, Subclass3Msg.class
        }
    )
    public SubclassMsg getOoo() {
        return ooo;
    }

    public void setOoo(SubclassMsg ooo) {
        this.ooo = ooo;
    }

    @SchemaElement(title = "oooaaa")
    @SchemaArrayType(
        isNullable = false,
        isElementNullable = false,
        elementTypes =  {
            Subclass2Msg.class, Subclass3Msg.class
        }
    )
    public ObjectArrayList<SubclassMsg> getOooaaa() {
        return oooaaa;
    }

    public void setOooaaa(ObjectArrayList<SubclassMsg> oooaaa) {
        this.oooaaa = oooaaa;
    }
}
