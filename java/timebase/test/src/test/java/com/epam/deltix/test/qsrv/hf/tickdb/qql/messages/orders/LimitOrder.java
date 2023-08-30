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

package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders;

import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SchemaElement(name = "deltix.orders.LimitOrder")
public class LimitOrder extends Order {

    private LimitOrderInfo info;
    private ObjectArrayList<Execution> executions = new ObjectArrayList<>();
    private ObjectArrayList<CharSequence> customTags = new ObjectArrayList<>();

    @SchemaElement(title = "info")
    public LimitOrderInfo getInfo() {
        return info;
    }

    public void setInfo(LimitOrderInfo info) {
        this.info = info;
    }

    @SchemaElement(title = "executions")
    public ObjectArrayList<Execution> getExecutions() {
        return executions;
    }

    public void setExecutions(ObjectArrayList<Execution> executions) {
        this.executions = executions;
    }

    @SchemaElement
    public ObjectArrayList<CharSequence> getCustomTags() {
        return customTags;
    }

    public void setCustomTags(ObjectArrayList<CharSequence> customTags) {
        this.customTags = customTags;
    }

    @Override
    public String toString() {
        return "LimitOrder{" +
            "id=" + id +
            '}';
    }
}
