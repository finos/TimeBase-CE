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
package com.epam.deltix.qsrv.hf.pub.md.json;

import java.util.List;

/**
 * Created by Alex Karpovich on 13/11/2020.
 */
public class DataTypeDef {

    public DataTypeDef() { // for serialization
    }

    public DataTypeDef(String name, String encoding, boolean nullable) {
        this.encoding = encoding;
        this.nullable = nullable;
        this.name = name;
    }

    private String encoding;
    private boolean nullable;
    private String name;
    private List<String> types;
    private DataTypeDef elementType;

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public DataTypeDef getElementType() {
        return elementType;
    }

    public void setElementType(DataTypeDef elementType) {
        this.elementType = elementType;
    }
}