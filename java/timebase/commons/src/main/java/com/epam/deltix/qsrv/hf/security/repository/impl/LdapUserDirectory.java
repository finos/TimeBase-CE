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
package com.epam.deltix.qsrv.hf.security.repository.impl;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.epam.deltix.qsrv.hf.security.LDAPTools;
import com.epam.deltix.qsrv.hf.security.SecurityConfigurator;
import com.epam.deltix.qsrv.hf.security.repository.GroupEntity;
import com.epam.deltix.qsrv.hf.security.repository.impl.LdapDirectoryUtils.SecurityEntityType;
import com.epam.deltix.qsrv.hf.security.repository.UserDirectory;
import com.epam.deltix.qsrv.hf.security.repository.UserEntity;
import com.epam.deltix.qsrv.hf.security.repository.impl.LdapDirectoryUtils.LdapAttribute;
import com.epam.deltix.util.collections.CollectionUtil;

import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.ldap.LDAPConnection.Vendor;
import com.epam.deltix.util.ldap.security.Configuration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Description: LdapUserDirectory
 * Date: Feb 28, 2011
 *
 * @author Nickolay Dul
 */
public class LdapUserDirectory implements UserDirectory {
    public static final Logger LOGGER = LdapDirectoryUtils.LOGGER;

    private final Configuration configuration;
    private final DirContext context;
    private String userNode = SecurityConfigurator.DEFAULT_USERS_ENTRY;

    public LdapUserDirectory(Configuration configuration) {
        this.configuration = configuration;

        try {
            context = connect();
            initialize();
        } catch (NamingException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    private DirContext connect() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();

        // configure our directory context environment.
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_PRINCIPAL, configuration.credentials.name);
        env.put(Context.SECURITY_CREDENTIALS, configuration.credentials.get());
        env.put(Context.PROVIDER_URL, StringUtils.join(" ", Arrays.copyOf(configuration.connection.toArray(), configuration.connection.size(), String[].class)));

        // ensure that we have a directory context available
        return new InitialDirContext(env);
    }

    public void initialize() {
        try {
            LdapDirectoryUtils.initializeUserDirectory(context, configuration, userNode);
        } catch (NamingException e) {
            throw new RuntimeException("User Directory initialization failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            if (context != null)
                context.close();
        } catch (NamingException e) {
            Util.handleException(e);
        }
    }

    ///////////////////// UserDirectory IMPL ///////////////////

    @Override
    public Collection<UserEntity> listUsers() {
        try {
            return getUsers(context);
        } catch (NamingException e) {
            throw new RuntimeException("Could not load users: " + e.getMessage(), e);
        }
    }

    @Override
    public UserEntity getUserById(String userId) {
        try {
            String user = StringUtils.trim(userId);
            if (user == null)
                throw new NullPointerException("User ID could not be empty!");
            // lookup user
            return lookupUserEntity(context, user);
        } catch (InvalidNameException e) {
            LOGGER.severe("User lookup failure: " + e.getMessage());
            throw new IllegalStateException("User [" + userId + "] does not exist!");
        } catch (NameNotFoundException e) {
            return null;
        } catch (NamingException e) {
            throw new RuntimeException("User lookup failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void createUser(UserEntity user) {
        try {
            createUserEntry(context, user);
        } catch (NameAlreadyBoundException e) {
            LOGGER.severe("User creation failure: " + e.getMessage());
            throw new IllegalStateException("User [" + user.getId() + "] already exists!");
        } catch (NamingException e) {
            throw new RuntimeException("User creation failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void modifyUser(UserEntity user) {
        try {
            modifyUserEntry(context, user);
        } catch (NameNotFoundException e) {
            LOGGER.severe("User modification failure: " + e.getMessage());
            throw new IllegalStateException("User [" + user.getId() + "] does not exist!");
        } catch (NamingException e) {
            throw new RuntimeException("User modification failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteUser(String userId) {
        try {
            String user = StringUtils.trim(userId);
            if (user == null)
                throw new NullPointerException("User ID could not be empty!");
            // delete user
            deleteUserEntry(context, user);
        } catch (NamingException e) {
            throw new RuntimeException("User deletion failure: " + e.getMessage(), e);
        }
    }

    @Override
    public Collection<GroupEntity> listGroups() {
        try {
            return LdapDirectoryUtils.listGroups(context, configuration, null);
        } catch (NamingException e) {
            throw new RuntimeException("Could not load groups: " + e.getMessage(), e);
        }
    }

    @Override
    public GroupEntity getGroupById(String groupId) {
        try {
            String group = StringUtils.trim(groupId);
            if (group == null)
                throw new NullPointerException("Group ID could not be empty!");
            // lookup group
            return lookupGroupEntity(context, group);
        } catch (InvalidNameException e) {
            LOGGER.severe("Group lookup failure: " + e.getMessage());
            throw new IllegalStateException("Group [" + groupId + "] does not exist!");
        } catch (NameNotFoundException e) {
            return null;
        } catch (NamingException e) {
            throw new RuntimeException("Group lookup failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void createGroup(GroupEntity group) {
        try {
            createGroupEntry(context, group);
        } catch (NameAlreadyBoundException e) {
            LOGGER.severe("Group creation failure: " + e.getMessage());
            throw new IllegalStateException("Group [" + group.getId() + "] already exists!");
        } catch (NamingException e) {
            throw new RuntimeException("Group creation failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void modifyGroup(GroupEntity group) {
        try {
            modifyGroupEntry(context, group);
        } catch (NameNotFoundException e) {
            LOGGER.severe("Group modification failure: " + e.getMessage());
            throw new IllegalStateException("Group [" + group.getId() + "] does not exist!");
        } catch (NamingException e) {
            throw new RuntimeException("Group modification failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void addGroupMembers(String group, Set<String> members) {
        try {
            modifyGroupMembers(context, DirContext.ADD_ATTRIBUTE, group, members);
        } catch (NameNotFoundException e) {
            LOGGER.severe("Group modification failure: " + e.getMessage());
            throw new IllegalStateException("Group [" + group + "] does not exist!");
        } catch (NamingException e) {
            throw new RuntimeException("Group modification failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeGroupMembers(String group, Set<String> members) {
        try {
            modifyGroupMembers(context, DirContext.REMOVE_ATTRIBUTE, group, members);
        } catch (NameNotFoundException e) {
            LOGGER.severe("Group modification failure: " + e.getMessage());
            throw new IllegalStateException("Group [" + group + "] does not exist!");
        } catch (NamingException e) {
            throw new RuntimeException("Group modification failure: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteGroup(String groupId) {
        try {
            String group = StringUtils.trim(groupId);
            if (group == null)
                throw new NullPointerException("Group ID could not be empty!");
            // delete group
            deleteGroupEntry(context, group);
        } catch (NamingException e) {
            throw new RuntimeException("Group deletion failure: " + e.getMessage(), e);
        }
    }


    ////////////////////////// NODES HANDLING ///////////////////////

    @Override
    public void setUserNode(String userNodeId) {
        userNode = userNodeId;
    }

    @Override
    public String getUserNode() {
        return userNode;
    }

    @Override
    public String getGroupNode() {
        return configuration.groups.get(0).node;
    }

    ////////////////////////// USERS HANDLING ///////////////////////

    private void createUserEntry(DirContext context, UserEntity user) throws NamingException {
        if (StringUtils.trim(user.getPassword()) == null)
            throw new IllegalArgumentException("User password could not be empty!");
        // validate user
        validateUser(context, user);

        // create and populate user attributes
        Attributes attributes = LdapDirectoryUtils.toAttributes(user, configuration);
        // create user entry
        LdapName userLdapName = getUserLdapName(user.getId());
        context.createSubcontext(userLdapName, attributes);

        // add new user as groups member
        Set<String> userGroups = user.getUserGroups();
        if (!userGroups.isEmpty())
            modifyMembersInGroups(context, DirContext.ADD_ATTRIBUTE, userGroups, userLdapName);
    }

    private void modifyUserEntry(DirContext context, UserEntity modifiedUser) throws NamingException {
        // validate user
        validateUser(context, modifiedUser);

        UserEntity prevUser = lookupUserEntity(context, modifiedUser.getId());

        ModificationItem[] items =
                LdapDirectoryUtils.toModificationItems(prevUser, modifiedUser, configuration);
        if (items.length <= 0)
            return; // no any changes

        LdapName userLdapName = getUserLdapName(modifiedUser.getId());
        context.modifyAttributes(userLdapName, items);

        Set<String> prevGroups = prevUser.getUserGroups();
        Set<String> newGroups = modifiedUser.getUserGroups();
        if(!newGroups.isEmpty()) {
            if (!prevGroups.isEmpty()) {
                prevGroups.removeAll(newGroups); // group members to remove
                newGroups.removeAll(prevUser.getUserGroups()); // group members to add
                modifyMembersInGroups(context, DirContext.REMOVE_ATTRIBUTE, prevGroups, userLdapName);
            }
            modifyMembersInGroups(context, DirContext.ADD_ATTRIBUTE, newGroups, userLdapName);
        }
    }

    private void deleteUserEntry(DirContext context, String userId) throws NamingException {
        // ND TODO: check if exist groups which contains ONLY given user as member
        // destroy user entry
        LdapName userLdapName = getUserLdapName(userId);
        context.destroySubcontext(userLdapName);

        // remove all group members attributes for given user
        Set<String> groupIds = findGroupsForMember(context, userLdapName);
        if (!groupIds.isEmpty())
            modifyMembersInGroups(context, DirContext.REMOVE_ATTRIBUTE, groupIds, userLdapName);
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    private List<UserEntity> getUsers(DirContext context) throws NamingException {
        // set up parameters for an appropriate search
        String filter = "( cn = *)";
        if (configuration.vendor.equals(Vendor.ApacheDS)) {
            filter = "(" + LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.UserId) + "=*)";
        } else if (configuration.vendor.equals(Vendor.ApacheDS)) {
            filter = "(&(" + LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.UserId) + "=*)(objectCategory=user))";
        }
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        controls.setReturningAttributes(LdapDirectoryUtils.getAttributeIds(SecurityEntityType.USER, configuration));

        // perform the configured search and process the results
        NamingEnumeration<SearchResult> searchResults = context.search(LDAPTools.escapeDN(userNode), LDAPTools.escapeLDAPSearchFilter(filter), controls);

        List<UserEntity> result = new ArrayList<>(5);
        while (searchResults.hasMore()) {
            Attributes attributes = searchResults.next().getAttributes();
            LOGGER.log(Level.FINER, "[LDAP] User attributes: {0}", attributes);
            if (attributes != null) {
                String userId = LdapDirectoryUtils.getStringValue(attributes, LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.UserId), null);
                // find user group membership
                Set<String> groupIds = findGroupsForMember(context, getUserLdapName(userId));
                // build user entity
                result.add(LdapDirectoryUtils.parseUserEntity(attributes, groupIds, configuration));
            }
        }
        return result;
    }

    private UserEntity lookupUserEntity(DirContext context, String userId) throws NamingException {
        LdapName userLdapName = getUserLdapName(userId);
        Attributes attributes = context.getAttributes(userLdapName, LdapDirectoryUtils.getAttributeIds(SecurityEntityType.USER, configuration));
        // find user group membership
        Set<String> groupIds = findGroupsForMember(context, userLdapName);
        // build user entity
        return LdapDirectoryUtils.parseUserEntity(attributes, groupIds, configuration);
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    private void validateUser(DirContext context, UserEntity user) throws NamingException {
        if (StringUtils.trim(user.getSecondName()) == null)
            throw new IllegalStateException("User's second name could not be empty!");

        Set<String> groups = user.getUserGroups();
        if (!groups.isEmpty()) {
            String groupIdAttr = LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.GroupId);
            Set<String> foundGroupIds = findEntityIds(context, new LdapName(LDAPTools.escapeDN(configuration.groups.get(0).node)), groups, groupIdAttr);
            // remove found groups
            groups.removeAll(foundGroupIds);
            if (!groups.isEmpty())
                throw new IllegalStateException("Unknown groups: " + CollectionUtil.toString(groups, "[", "]", ", "));
        }
    }

    ////////////////////////// GROUPS HANDLING ///////////////////////

    private void createGroupEntry(DirContext context, GroupEntity group) throws NamingException {
        // validate group
        Set<String> members = group.getMembers();


        if (members.isEmpty())
            throw new IllegalArgumentException("Group should contain at least one member!");
        validateGroup(context, group);

        // create and populate group attributes
        Attributes attributes = LdapDirectoryUtils.toAttributes(context, group, configuration);
        // create group entry
        LdapName groupLdapName = getGroupLdapName(group.getId());
        context.createSubcontext(groupLdapName, attributes);
    }

    private void modifyGroupEntry(DirContext context, GroupEntity modifiedGroup) throws NamingException {
        // validate group
        Set<String> members = modifiedGroup.getMembers();


        if (!members.isEmpty())
            validateGroup(context, modifiedGroup);

        GroupEntity prevGroup = lookupGroupEntity(context, modifiedGroup.getId());

        ModificationItem[] items =
                LdapDirectoryUtils.toModificationItems(context, prevGroup, modifiedGroup, configuration);
        if (items.length <= 0)
            return; // no any changes

        LdapName groupLdapName = getGroupLdapName(modifiedGroup.getId());
        context.modifyAttributes(groupLdapName, items);
    }

    private void modifyGroupMembers(DirContext context, int action, String groupName, Set<String> members) throws NamingException {
        if (members.size() <= 0) {
            throw new IllegalArgumentException("Principal's list can't be empty");
        }
        Set<String> groups = LdapDirectoryUtils.detectGroups(context, configuration, members);
        Set<String> users = LdapDirectoryUtils.detectUsers(context, configuration, members);

        LdapName[] names = new LdapName[groups.size() + users.size()];
        int i = 0;
        for (String group : groups) {
            names[i++] = getGroupLdapName(group);
        }
        for (String user : users) {
            names[i++] = getUserLdapName(user);
        }
        if (names.length == 0) {
            throw new IllegalArgumentException("List of principals doesn't contain valid principals");
        }
        modifyMembersInGroup(context, action, groupName, names);
    }

    private void deleteGroupEntry(DirContext context, String groupId) throws NamingException {
        // ND TODO: check if exist groups which contains ONLY given group as member 
        // destroy group entry
        LdapName groupLdapName = getGroupLdapName(groupId);
        context.destroySubcontext(groupLdapName);

        // remove target group from other groups
        Set<String> groupIds = findGroupsForMember(context, groupLdapName);
        if (!groupIds.isEmpty())
            modifyMembersInGroups(context, DirContext.REMOVE_ATTRIBUTE, groupIds, groupLdapName);
    }

    private GroupEntity lookupGroupEntity(DirContext context, String groupId) throws NamingException {
        LdapName groupLdapName = getGroupLdapName(groupId);
        Attributes attributes = context.getAttributes(groupLdapName, LdapDirectoryUtils.getAttributeIds(SecurityEntityType.GROUP, configuration));
        if (attributes == null || attributes.size() <= 0)
            throw new IllegalArgumentException("Group with ID [" + groupId + "] does not exist!");
        return LdapDirectoryUtils.parseGroupEntity(attributes, configuration, null);
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    private void validateGroup(DirContext context, GroupEntity group) throws NamingException {
        Set<String> members = group.getMembers();

        if (members.contains(group.getId()))
            throw new IllegalStateException("Group could not contain itself as member!");

        String groupIdAttr = LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.GroupId);
        Set<String> foundGroupIds = findEntityIds(context, new LdapName(LDAPTools.escapeDN(configuration.groups.get(0).node)), members, groupIdAttr);
        // remove found groups
        members.removeAll(foundGroupIds);

        if (!members.isEmpty()) {
            String userIdAttr = LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.UserId);
            // find users
            Set<String> foundUserIds = findEntityIds(context, new LdapName(LDAPTools.escapeDN(userNode)), members, userIdAttr);
            // remove found users
            members.removeAll(foundUserIds);
            if (!members.isEmpty())
                throw new IllegalStateException("Unknown group members : " + CollectionUtil.toString(members, "[", "]", ", "));
        }

        // ND TODO: check cyclic references in groups
    }

    ////////////////////////// UTILITIES /////////////////////////

    public static Set<String> findEntityIds(DirContext context, LdapName searchNode, Set<String> entityIds, String entityIdAttr) throws NamingException {
        String[] groupIds = entityIds.toArray(new String[entityIds.size()]);
        // build filter
        StringBuilder filter = new StringBuilder("(|");
        for (int i = 0; i < groupIds.length; i++)
            filter.append("(").append(entityIdAttr).append("={").append(i).append("}").append(")");
        filter.append(")");
        // find users
        return LdapDirectoryUtils.searchEntryNames(context, searchNode, entityIdAttr, filter.toString(), groupIds);
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    private LdapName getUserLdapName(String userId) throws InvalidNameException {
        String userIdAttr = LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.UserId);
        return LdapDirectoryUtils.buildLdapName(userIdAttr, userId, new LdapName(LDAPTools.escapeDN(userNode)));
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    private LdapName getGroupLdapName(String groupId) throws InvalidNameException {
        String groupIdAttr = LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.GroupId);
        return LdapDirectoryUtils.buildLdapName(groupIdAttr, groupId, new LdapName(LDAPTools.escapeDN(configuration.groups.get(0).node)));
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    private Set<String> findGroupsForMember(DirContext context, LdapName memberLdapName) throws NamingException {
        String groupMemberAttr = LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.GroupMember);
        return LdapDirectoryUtils.searchEntryNames(context, new LdapName(LDAPTools.escapeDN(configuration.groups.get(0).node)),
                LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.GroupId),
                "(" + groupMemberAttr + "={0})", memberLdapName.toString());
    }

    private void modifyMembersInGroups(DirContext context,
                                       int modifyAction,
                                       Set<String> groupIds,
                                       LdapName... memberLdapNames) throws NamingException {
        String groupMemberAttr = LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.GroupMember);
        for (String groupId : groupIds) {
            Attributes attributes = new BasicAttributes(true);
            for (LdapName ldapName : memberLdapNames)
                attributes.put(new BasicAttribute(groupMemberAttr, ldapName.toString()));
            context.modifyAttributes(getGroupLdapName(groupId), modifyAction, attributes);
        }
    }

    private void modifyMembersInGroup(DirContext context,
                                      int modifyAction,
                                      String groupId,
                                      LdapName... memberLdapNames) throws NamingException {
        String groupMemberAttr = LdapDirectoryUtils.getAttributeById(configuration, LdapAttribute.GroupMember);
        ModificationItem[] mods = new ModificationItem[memberLdapNames.length];
        for (int i = 0; i < memberLdapNames.length; i++)
            mods[i] = new ModificationItem(modifyAction, new BasicAttribute(groupMemberAttr, memberLdapNames[i].toString()));
        context.modifyAttributes(getGroupLdapName(groupId), mods);

    }
}