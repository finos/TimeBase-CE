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
package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class ModuleIdentityDefinition extends ObjectDefinition {
    public final String             lastUpdated;
    public final String             organization;
    public final String             contactInfo;
    public final Revision []        revisions;

    public ModuleIdentityDefinition (
        long                        location, 
        String                      id,
        String                      lastUpdated, 
        String                      organization,
        String                      contactInfo, 
        String                      description, 
        Revision []                 revisions,
        OIDValue                    value
    ) 
    {
        super (location, id, description, value);
        
        this.lastUpdated = lastUpdated;
        this.organization = organization;
        this.contactInfo = contactInfo;
        this.revisions = revisions;
    }           
}