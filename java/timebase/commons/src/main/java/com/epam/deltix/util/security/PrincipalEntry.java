package com.epam.deltix.util.security;

import java.security.Principal;

public abstract class PrincipalEntry extends Entry implements Principal {

    private String description;
    private String name;

    public PrincipalEntry(String id) {
        super(id);
        name = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrincipalEntry that = (PrincipalEntry) o;

        return id.toLowerCase().equals(that.id.toLowerCase());
    }

    @Override
    public int hashCode() {
        return id.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
