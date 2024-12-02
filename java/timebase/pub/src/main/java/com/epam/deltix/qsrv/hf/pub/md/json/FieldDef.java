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

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/**
 * Schema field definition.
 */
public class FieldDef {

    public FieldDef() {
    }

    /**
     * Default visibility state.
     */
    @JsonProperty("hide")
    private boolean hidden = false;

    /**
     * Field Name.
     */
    @JsonProperty
    private String name;

    /**
     * Field Title.
     */
    @JsonProperty
    private String title;

    /**
     * Field description
     */
    @JsonProperty
    private String description;

    /**
     * Field Data Type.
     */
    @JsonProperty
    private DataTypeDef type;

    /**
     * Static fields indicator
     */
    @JsonProperty(value = "static")
    private boolean isStatic = false;

    @JsonProperty
    @Nullable
    private String value;

    private boolean primaryKey;
    private String relativeTo;

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DataTypeDef getType() {
        return type;
    }

    public void setType(DataTypeDef type) {
        this.type = type;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public String       getDescription() {
        return description;
    }

    public void         setDescription(String description) {
        this.description = description;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public void setValue(@Nullable String value) {
        this.value = value;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getRelativeTo() {
        return relativeTo;
    }

    public void setRelativeTo(String relativeTo) {
        this.relativeTo = relativeTo;
    }

    public static FieldDef createNonStatic(String name, String title, String description, DataTypeDef type, String relativeTo, boolean pk) {
        FieldDef fieldDef = new FieldDef();
        fieldDef.name = name;
        fieldDef.type = type;
        fieldDef.title = title;
        fieldDef.isStatic = false;
        fieldDef.description = description;
        fieldDef.relativeTo = relativeTo;
        fieldDef.primaryKey = pk;
        return fieldDef;
    }

    public static FieldDef createStatic(String name, String title, String description, DataTypeDef type, String value) {
        FieldDef fieldDef = new FieldDef();
        fieldDef.name = name;
        fieldDef.type = type;
        fieldDef.title = title;
        fieldDef.isStatic = true;
        fieldDef.value = value;
        fieldDef.description = description;
        return fieldDef;
    }
}