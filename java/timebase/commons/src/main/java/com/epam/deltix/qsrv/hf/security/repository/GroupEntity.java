package com.epam.deltix.qsrv.hf.security.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: GroupEntity
 * Date: Feb 28, 2011
 *
 * @author Nickolay Dul
 */
public class GroupEntity extends SecurityEntity {

    private HashSet<String> members = new HashSet<String>();


    public GroupEntity(String entityId) {
        super(entityId, null);
    }

    public GroupEntity(String entityId, String description,
                       Set<String> members) {
        super(entityId, description);
        if (members != null && !members.isEmpty())
            this.members.addAll(members);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getMembers() {
        return members.isEmpty() ? Collections.<String>emptySet() : (Set<String>) members.clone();
    }

    public void addMembers(String... members) {
        Collections.addAll(this.members, members);
    }

    public void addMembers(Collection<String> members) {
        this.members.addAll(members);
    }

    public void clearMembers() {
        members.clear();
    }



}
