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
