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
package com.epam.deltix.qsrv.snmp.model.timebase;

import com.epam.deltix.snmp.pub.Description;
import com.epam.deltix.snmp.pub.Id;
import com.epam.deltix.snmp.pub.Index;

/**
 *
 */
@Description ("Information about failure")
public interface Failure {

    @Id(1) @Index
    @Description("Index")
    public int                      getIndex();

    @Id(2)
    @Description("Error Message")
    public String                  getMessage();
}