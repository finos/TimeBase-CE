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
package com.epam.deltix.snmp.smi;

/**
 *
 */
public class SMINamedType extends SMIType {
    public static final SMINamedType      DisplayString_INSTANCE = 
        new SMINamedType (SMIOctetStringType.INSTANCE, "DisplayString");
    
    public static final SMINamedType      Integer32_INSTANCE = 
        new SMINamedType (SMIIntegerType.INSTANCE, "Integer32");
    
    private final SMIType           base;    
    private final String            name;
    
    public SMINamedType (SMIType base, String name) {
        this.base = base;    
        this.name = name;
    }

    public SMIType              getBase () {
        return base;
    }

    public String               getName () {
        return name;
    }        
}