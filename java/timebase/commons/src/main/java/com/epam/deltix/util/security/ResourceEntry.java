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
package com.epam.deltix.util.security;

import com.epam.deltix.util.security.AccessControlRule.ResourceFormat;
import com.epam.deltix.util.security.AccessControlRule.ResourceType;

public class ResourceEntry extends Entry {
    private String resource;
    private ResourceType resourceType = ResourceType.Principal;
    private ResourceFormat resourceFormat = ResourceFormat.Text;

    public ResourceEntry(String id) {
        super(id);
    }

    public ResourceEntry(String resource, ResourceType resourceType, ResourceFormat resourceFormat) {
        super(resource + ":" + resourceType + ":" + resourceFormat);
        this.resource = resource;
        this.resourceType = resourceType;
        this.resourceFormat = resourceFormat;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public ResourceFormat getResourceFormat() {
        return resourceFormat;
    }

    public void setResourceFormat(ResourceFormat resourceFormat) {
        this.resourceFormat = resourceFormat;
    }
}