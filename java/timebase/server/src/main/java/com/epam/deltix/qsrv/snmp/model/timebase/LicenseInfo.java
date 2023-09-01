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

/**
 *
 */
public interface LicenseInfo {

    @Id(1)
    @Description("Date until license is valid")
    public String   getValidUtil();

    @Id(2)
    @Description("Licensee information")
    public String   getLicensee();

    @Id(3)
    @Description("Last time if license validation")
    public String   getLastValidated();

    @Id(4)
    @Description("Number of days license ")
    public int      getDaysValid();

    @Id(5)
    @Description("License state")
    public String   getLicenseState();

    @Id(6)
    @Description("License type")
    public String   getType();

    @Id(7)
    @Description("License Feature")
    public String   getFeatures();
}