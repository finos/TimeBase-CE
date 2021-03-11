package com.epam.deltix.qsrv.hf.security.repository;

import java.util.Collection;
import java.util.Set;

import com.epam.deltix.util.lang.Disposable;

public interface UserDirectory extends Disposable {

    Collection<UserEntity> listUsers();
    UserEntity getUserById(String userId);

    void createUser(UserEntity user);
    void modifyUser(UserEntity user);
    void deleteUser(String userId);

    Collection<GroupEntity> listGroups();
    GroupEntity getGroupById(String groupId);

    void createGroup(GroupEntity group);
    void modifyGroup(GroupEntity group);
    void addGroupMembers(String groupId,Set<String> members);
    void removeGroupMembers(String groupId,Set<String> members);
    void deleteGroup(String groupId);

    void setUserNode(String userNodeId);

    String getUserNode();
    String getGroupNode();
}
