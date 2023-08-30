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

@SchemaElement(name = "deltix.orders.MarketOrder")
public class MarketOrder extends Order {

    private MarketOrderInfo info;
    private Execution execution;

    @SchemaElement(title = "info")
    public MarketOrderInfo getInfo() {
        return info;
    }

    public void setInfo(MarketOrderInfo info) {
        this.info = info;
    }

    @SchemaElement(title = "execution")
    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    @Override
    public String toString() {
        return "MarketOrder{" +
            "id=" + id +
            '}';
    }
}
