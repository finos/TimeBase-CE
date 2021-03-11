package com.epam.deltix.util.security;

public abstract class Entry {

    public final String    id;

    protected Entry(String id) {
        this.id = id;
    }

    public String       getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entry that = (Entry) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
