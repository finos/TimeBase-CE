package com.epam.deltix.util.security;

import java.util.ArrayList;
import java.util.List;

public class GroupEntry extends PrincipalEntry {
    private final List<PrincipalEntry> principals = new ArrayList<>();

    public GroupEntry(String id) {
        super(id);
    }

    public PrincipalEntry[] getPrincipals() {
        return principals.toArray(new PrincipalEntry[principals.size()]);
    }

    public void addPrincipal(PrincipalEntry member) {
        if (!principals.contains(member))
            principals.add(member);
    }

    public void removePrincipal(PrincipalEntry member) {
        principals.remove(member);
    }
}
