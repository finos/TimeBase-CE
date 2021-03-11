package com.epam.deltix.qsrv.hf.security.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.epam.deltix.util.lang.StringUtils;

/**
 * Description: UserEntity
 * Date: Feb 28, 2011
 *
 * @author Nickolay Dul
 */
public class UserEntity extends SecurityEntity {
    private String password;
    private String firstName;
    private String secondName;
    private HashSet<String> userGroups = new HashSet<String>();

    public UserEntity(String entityId) {
        super(entityId, null);
    }

    public UserEntity(String entityId, String password, String firstName, String secondName, String description, Set<String> userGroups) {
        super(entityId, description);
        this.password = password;
        this.firstName = firstName;
        if (secondName == null)
            this.secondName = entityId;
        else
            this.secondName = secondName;

        if (userGroups != null && !userGroups.isEmpty())
            this.userGroups.addAll(userGroups);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getUserGroups() {
        return userGroups.isEmpty() ? Collections.<String>emptySet() : (Set<String>) userGroups.clone();
    }

    public void addUserGroups(String... groups) {
        Collections.addAll(userGroups, groups);
    }

    public void addUserGroups(Collection<String> groups) {
        userGroups.addAll(groups);
    }

    public void clearUserGroups() {
        userGroups.clear();
    }
}
