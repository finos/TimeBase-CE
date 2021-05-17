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

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.epam.deltix.qsrv.hf.security.LDAPTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.collections.CollectionUtils;

import com.epam.deltix.qsrv.hf.security.SecurityConfigurator;
import com.epam.deltix.qsrv.hf.security.repository.GroupEntity;
import com.epam.deltix.qsrv.hf.security.repository.UserDirectory;
import com.epam.deltix.qsrv.hf.security.repository.UserEntity;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Transformer;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.ldap.LDAPConnection.Vendor;
import com.epam.deltix.util.ldap.security.Configuration;

public final class LdapDirectoryUtils {

    public static enum SecurityEntityType {
        USER, GROUP
    }

    public static enum LdapAttribute {
        UserId, UserPassword, UserFirstName, UserSecondName, UserDescription,
        GroupId, GroupMember, GroupDescription;

        public static final EnumSet<LdapAttribute> USER_ATTRIBUTES =
                EnumSet.of(UserId, UserPassword, UserFirstName, UserSecondName, UserDescription);
        public static final EnumSet<LdapAttribute> GROUP_ATTRIBUTES =
                EnumSet.of(GroupId, GroupMember, GroupDescription);

    }

    public static final Logger LOGGER = Logger.getLogger(UserDirectory.class.getName());

    public static final String[] DEFAULT_USER_OBJCLASSES = {"top", "person", "organizationalPerson", "inetOrgPerson"};
    public static final String[] DEFAULT_GROUP_OBJCLASSES = {"top", "groupOfUniqueNames"};
    //created to support AD
    public static final String[] DEFAULT_AD_USER_OBJCLASSES = {"top", "user"};
    public static final String[] DEFAULT_AD_GROUP_OBJCLASSES = {"top", "group"};

    private static final EnumMap<LdapAttribute, String> DEFAULT_ATTR_MAP = new EnumMap<>(LdapAttribute.class);

    static {
        DEFAULT_ATTR_MAP.put(LdapAttribute.UserId, "cn");
        DEFAULT_ATTR_MAP.put(LdapAttribute.UserPassword, "userPassword");
        DEFAULT_ATTR_MAP.put(LdapAttribute.UserFirstName, "givenName");
        DEFAULT_ATTR_MAP.put(LdapAttribute.UserSecondName, "sn");
        DEFAULT_ATTR_MAP.put(LdapAttribute.UserDescription, "description");

        DEFAULT_ATTR_MAP.put(LdapAttribute.GroupId, "cn");
        DEFAULT_ATTR_MAP.put(LdapAttribute.GroupMember, "uniqueMember");
        DEFAULT_ATTR_MAP.put(LdapAttribute.GroupDescription, "description");

    }

    //Created to support Active Directory
    private static final EnumMap<LdapAttribute, String> DEFAULT_AD_ATTR_MAP = new EnumMap<>(LdapAttribute.class);

    static {
        DEFAULT_AD_ATTR_MAP.put(LdapAttribute.UserId, "cn");
        DEFAULT_AD_ATTR_MAP.put(LdapAttribute.UserPassword, "userPassword");
        DEFAULT_AD_ATTR_MAP.put(LdapAttribute.UserFirstName, "givenName");
        DEFAULT_AD_ATTR_MAP.put(LdapAttribute.UserSecondName, "sn");
        DEFAULT_AD_ATTR_MAP.put(LdapAttribute.UserDescription, "description");

        DEFAULT_AD_ATTR_MAP.put(LdapAttribute.GroupId, "cn");
        DEFAULT_AD_ATTR_MAP.put(LdapAttribute.GroupMember, "member");
        DEFAULT_AD_ATTR_MAP.put(LdapAttribute.GroupDescription, "description");

    }

    private static LdapName usersNode;

    public static String[] getAttributeIds(SecurityEntityType entityType, Configuration config) {
        switch (entityType) {
            case GROUP: {
                String[] result = new String[LdapAttribute.GROUP_ATTRIBUTES.size()];
                int i = 0;
                for (LdapAttribute attribute : LdapAttribute.GROUP_ATTRIBUTES)
                    result[i++] = getAttributeById(config, attribute);
                return result;
            }
            case USER: {
                String[] result = new String[LdapAttribute.USER_ATTRIBUTES.size()];
                int i = 0;
                for (LdapAttribute attribute : LdapAttribute.USER_ATTRIBUTES)
                    result[i++] = getAttributeById(config, attribute);
                return result;
            }
            default:
                throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }
    }

    /////////////////////////// USER UTILS /////////////////////////

    public static Attributes toAttributes(UserEntity user, Configuration config) throws NamingException {
        BasicAttributes attributes = new BasicAttributes();

        BasicAttribute objectclass = new BasicAttribute("objectclass");
        for (String objectclassName : getUserObjectClasses(config))
            objectclass.add(objectclassName);
        attributes.put(objectclass);

        addAttribute(attributes, getAttributeById(config, LdapAttribute.UserId), user.getId());
        addAttribute(attributes, getAttributeById(config, LdapAttribute.UserPassword), user.getPassword());
        addAttribute(attributes, getAttributeById(config, LdapAttribute.UserFirstName), user.getFirstName());
        addAttribute(attributes, getAttributeById(config, LdapAttribute.UserSecondName), user.getSecondName());
        addAttribute(attributes, getAttributeById(config, LdapAttribute.UserDescription), user.getDescription());

        return attributes;
    }

    public static UserEntity parseUserEntity(Attributes userAttributes,
                                             Set<String> userGroups,
                                             Configuration config) throws NamingException {
        String userId = getStringValue(userAttributes, getAttributeById(config, LdapAttribute.UserId), null);

        String description =
                getStringValue(userAttributes, getAttributeById(config, LdapAttribute.UserDescription), "");
        String firstName =
                getStringValue(userAttributes, getAttributeById(config, LdapAttribute.UserFirstName), "");
        String lastName =
                getStringValue(userAttributes, getAttributeById(config, LdapAttribute.UserSecondName), "");

        // do not read password
        return new UserEntity(userId, null, firstName, lastName, description, userGroups);
    }

    public static ModificationItem[] toModificationItems(UserEntity prevUser, UserEntity newUser,
                                                         Configuration config) throws NamingException {
        ArrayList<ModificationItem> modificationItems = new ArrayList<>(4);

        // special handling of password attribute - update only if not empty
        String password = StringUtils.trim(newUser.getPassword());
        if (password != null) {
            BasicAttribute passwordAttr = new BasicAttribute(getAttributeById(config, LdapAttribute.UserPassword), password);
            modificationItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, passwordAttr));
        }

        addModificationItem(modificationItems, getAttributeById(config, LdapAttribute.UserFirstName),
                newUser.getFirstName(), prevUser.getFirstName());
        addModificationItem(modificationItems, getAttributeById(config, LdapAttribute.UserSecondName),
                newUser.getSecondName(), prevUser.getSecondName());
        addModificationItem(modificationItems, getAttributeById(config, LdapAttribute.UserDescription),
                newUser.getDescription(), prevUser.getDescription());
        return modificationItems.toArray(new ModificationItem[modificationItems.size()]);
    }

    /////////////////////////// GROUP UTILS /////////////////////////
    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    public static List<GroupEntity> listGroups(DirContext context,
                                               Configuration config,
                                               Transformer<String, String> entityIdTransformer) throws NamingException {
        // set up parameters for an appropriate search
        String filter = "(" + DEFAULT_ATTR_MAP.get(LdapAttribute.GroupId) + "=*)";
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        controls.setReturningAttributes(getAttributeIds(SecurityEntityType.GROUP, config));

        // perform the configured search and process the results
        NamingEnumeration<SearchResult> searchResults = context.search(LDAPTools.escapeDN(config.groups.get(0).node), LDAPTools.escapeLDAPSearchFilter(filter), controls);

        List<GroupEntity> result = new ArrayList<>(5);
        while (searchResults.hasMore()) {
            Attributes attributes = searchResults.next().getAttributes();
            LOGGER.log(Level.FINER, "[LDAP] Group attributes: {0}", attributes);
            if (attributes != null)
                result.add(parseGroupEntity(attributes, config, entityIdTransformer));
        }
        return result;
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    public static Attributes toAttributes(DirContext context, GroupEntity user, Configuration config) throws NamingException {
        BasicAttributes attributes = new BasicAttributes();

        BasicAttribute objectclass = new BasicAttribute("objectclass");
        for (String objectclassName : getGroupObjectClasses(config))
            objectclass.add(objectclassName);
        attributes.put(objectclass);

        addAttribute(attributes, getAttributeById(config, LdapAttribute.GroupId), user.getId());
        addAttribute(attributes, getAttributeById(config, LdapAttribute.GroupDescription), user.getDescription());

        String memberAttr = getAttributeById(config, LdapAttribute.GroupMember);
        BasicAttribute memberAttribute = new BasicAttribute(memberAttr);

        Set<String> groups = detectGroups(context, config, user.getMembers());
        if (!groups.isEmpty())
            fillMemberAttribute(memberAttribute, getAttributeById(config, LdapAttribute.GroupId), new LdapName(LDAPTools.escapeDN(config.groups.get(0).node)), groups);

        Set<String> users = detectUsers(context, config, user.getMembers());
        if (!users.isEmpty())
            fillMemberAttribute(memberAttribute, getAttributeById(config, LdapAttribute.UserId), usersNode, users);

        if (memberAttribute.size() > 0)
            attributes.put(memberAttribute);

        return attributes;
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    public static GroupEntity parseGroupEntity(Attributes groupAttributes,
                                               Configuration config,
                                               Transformer<String, String> entityIdTransformer) throws NamingException {
        String groupId =
                getStringValue(groupAttributes, getAttributeById(config, LdapAttribute.GroupId), null);
        if (entityIdTransformer != null)
            groupId = entityIdTransformer.transform(groupId);
        if (groupId == null)
            throw new IllegalStateException("Group ID could not be empty!");

        String description =
                getStringValue(groupAttributes, getAttributeById(config, LdapAttribute.GroupDescription), "");

        // get all user groups
        Set<String> groupMembers = null;
        Attribute memberAttribute = groupAttributes.get(getAttributeById(config, LdapAttribute.GroupMember));
        if (memberAttribute != null) {
            groupMembers = new HashSet<>();

            NamingEnumeration<?> members = memberAttribute.getAll();
            while (members.hasMore()) {
                String memberDN = members.next().toString();
                LdapName memberLdapName = new LdapName(LDAPTools.escapeDN(memberDN));

                int cnIndex = memberLdapName.size() - 1;
                String memberId = StringUtils.trim(memberLdapName.getRdn(cnIndex).getValue().toString());
                if (entityIdTransformer != null)
                    memberId = entityIdTransformer.transform(memberId);

                memberLdapName.remove(cnIndex);

                if (memberLdapName.equals(usersNode) || memberLdapName.equals(new LdapName(LDAPTools.escapeDN(config.groups.get(0).node))))
                    groupMembers.add(memberId);

                else // in case of Active Directory group can contain members from other AD nodes 
                    LOGGER.log(Level.WARNING, "[LDAP] Group [{0}] contains invalid member path: {1}", new Object[]{groupId, memberDN});
            }
        }

        return new GroupEntity(groupId, description, groupMembers);
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    public static ModificationItem[] toModificationItems(DirContext context, GroupEntity prevGroup, GroupEntity newGroup,
                                                         Configuration config) throws NamingException {
        ArrayList<ModificationItem> modificationItems = new ArrayList<>(2);

        addModificationItem(modificationItems, getAttributeById(config, LdapAttribute.GroupDescription),
                newGroup.getDescription(), prevGroup.getDescription());

        String memberAttr = getAttributeById(config, LdapAttribute.GroupMember);

        // process group members
        Set<String> prevGroups = detectGroups(context, config, prevGroup.getMembers());
        Set<String> newGroups = detectGroups(context, config, newGroup.getMembers());
        if(!newGroups.isEmpty()) {
            String groupIdAttr = getAttributeById(config, LdapAttribute.GroupId);
            if (!prevGroups.isEmpty()) {
                prevGroups.removeAll(newGroups); // group members to remove
                newGroups.removeAll(prevGroup.getMembers()); // group members to add
                BasicAttribute membersToDelete = createMemberAttribute(memberAttr, groupIdAttr, new LdapName(LDAPTools.escapeDN(config.groups.get(0).node)), prevGroups);
                modificationItems.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, membersToDelete));
            }
            BasicAttribute membersToAdd = createMemberAttribute(memberAttr, groupIdAttr, new LdapName(LDAPTools.escapeDN(config.groups.get(0).node)), newGroups);
            modificationItems.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, membersToAdd));
        }

        // process user members
        Set<String> prevUsers = detectUsers(context, config, prevGroup.getMembers());
        Set<String> newUsers = detectUsers(context, config, newGroup.getMembers());
        if(!newUsers.isEmpty()) {
            String userIdAttr = getAttributeById(config, LdapAttribute.UserId);
            if (!prevUsers.isEmpty()) {
                prevUsers.removeAll(newUsers); // user members to remove
                newUsers.removeAll(prevGroup.getMembers()); // user members to add
                BasicAttribute membersToDelete = createMemberAttribute(memberAttr, userIdAttr, usersNode, prevUsers);
                modificationItems.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, membersToDelete));
            }
            BasicAttribute membersToAdd = createMemberAttribute(memberAttr, userIdAttr, usersNode, newUsers);
            modificationItems.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, membersToAdd));
        }

        return modificationItems.toArray(new ModificationItem[modificationItems.size()]);
    }

    @SuppressWarnings("unchecked")
    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    public static Set<String> detectGroups(DirContext context, Configuration config, Set<String> members) throws NamingException {
        Set<String> allgroups = searchEntryNames(context, new LdapName(LDAPTools.escapeDN(config.groups.get(0).node)), getAttributeById(config, LdapAttribute.GroupId), "(objectClass=" + getGroupObjectClasses(config)[getGroupObjectClasses(config).length - 1] + ")");
        if (allgroups == null)
            return null;
        return new HashSet<>(CollectionUtils.intersection(allgroups, members));
    }

    @SuppressWarnings("unchecked")
    public static Set<String> detectUsers(DirContext context, Configuration config, Set<String> members) throws NamingException {
        Set<String> allusers = searchEntryNames(context, usersNode, getAttributeById(config, LdapAttribute.UserId), "(objectClass=" + getUserObjectClasses(config)[getUserObjectClasses(config).length - 1] + ")");
        if (allusers == null)
            return null;
        return new HashSet<>(CollectionUtils.intersection(allusers, members));
    }

    ////////////////////////////// DIRECTORY UTILS ////////////////////////////

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    public static void initializeUserDirectory(DirContext context, Configuration config, String usersNode) throws NamingException {

        LdapDirectoryUtils.usersNode = new LdapName(LDAPTools.escapeDN(usersNode));

        String filter = "(" + LdapDirectoryUtils.usersNode.getRdn(2) + ")";
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        NamingEnumeration<SearchResult> enumresult = context.search(SecurityConfigurator.DEFAULT_DOMAIN, LDAPTools.escapeLDAPSearchFilter(filter), controls);
        if (!enumresult.hasMoreElements()) {
            LOGGER.log(Level.INFO, "[LDAP] Users base node [{0}] is not exists. Please set another one.", usersNode);
        }

        LdapName groupsNode = new LdapName(LDAPTools.escapeDN(config.groups.get(0).node));

        filter = "(" + groupsNode.getRdn(2) + ")";
        controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        enumresult = context.search(SecurityConfigurator.DEFAULT_DOMAIN, LDAPTools.escapeLDAPSearchFilter(filter), controls);
        if (!enumresult.hasMoreElements()) {
            LOGGER.log(Level.INFO, "[LDAP] Group base node [{0}] is not exists", groupsNode);
        }

    }

    ////////////////////////////// COMMON UTILS ////////////////////////////

    public static Set<String> searchEntryNames(DirContext context, LdapName searchNode, String returnedAttr, String filter, String... filterArgs) throws NamingException {
        return searchEntryNames(context, searchNode, returnedAttr, null, filter, filterArgs);
    }

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    public static Set<String> searchEntryNames(DirContext context,
                                               LdapName searchNode, String returnedAttr,
                                               Transformer<String, String> valueTransformer,
                                               String filter, String... filterArgs) throws NamingException {
        Set<String> result = new HashSet<>();

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        controls.setReturningAttributes(new String[]{returnedAttr});

        // perform the configured search and process the results
        NamingEnumeration<SearchResult> searchResults =
                context.search(searchNode, LDAPTools.escapeLDAPSearchFilter(filter), filterArgs, controls);

        while (searchResults.hasMore()) {
            SearchResult searchResult = searchResults.next();
            Attributes attrs = searchResult.getAttributes();
            if (attrs != null) {
                String value = getStringValue(attrs, returnedAttr, null);
                result.add(valueTransformer != null ? valueTransformer.transform(value) : value);
            }
        }
        return result;
    }

    private static BasicAttribute createMemberAttribute(String memberAttr, String valueAttrId,
                                                        LdapName memberBaseNode, Set<String> members) throws InvalidNameException {
        BasicAttribute memberAttribute = new BasicAttribute(memberAttr);
        fillMemberAttribute(memberAttribute, valueAttrId, memberBaseNode, members);
        return memberAttribute;
    }

    private static BasicAttribute fillMemberAttribute(BasicAttribute memberAttribute, String valueAttrId,
                                                      LdapName memberBaseNode, Set<String> members) throws InvalidNameException {
        for (String memberId : members) {
            LdapName groupLdapName = buildLdapName(valueAttrId, memberId, memberBaseNode);
            memberAttribute.add(groupLdapName.toString());
        }
        return memberAttribute;
    }

    public static LdapName buildLdapName(String key, String value, LdapName parent) throws InvalidNameException {
        LdapName result = (LdapName) parent.clone();
        result.add(new Rdn(key, value));
        return result;
    }

    private static void addAttribute(Attributes attributes, String attributeId, String value) {
        String trimmed = StringUtils.trim(value);
        if (trimmed != null)
            attributes.put(new BasicAttribute(attributeId, trimmed));
    }

    private static void addModificationItem(List<ModificationItem> items, String attributeId,
                                            String newValue, String prevValue) {
        String trimmedNew = StringUtils.trim(newValue);
        String trimmedPrev = StringUtils.trim(prevValue);
        if (Util.xequals(trimmedNew, trimmedPrev))
            return;
        if (trimmedNew == null)
            items.add(new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(attributeId, trimmedPrev)));
        else if (prevValue == null)
            items.add(new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(attributeId, trimmedNew)));
        else
            items.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attributeId, trimmedNew)));
    }

    public static String getStringValue(Attributes attrs, String attrID, String defValue) throws NamingException {
        Attribute attribute = attrs.get(attrID);
        if (attribute == null)
            return defValue;
        String result = StringUtils.trim(attribute.get().toString());
        return result == null ? defValue : result;
    }

    public static String[] getUserObjectClasses(Configuration config) {
        if (config.vendor == Vendor.ApacheDS)
            return LdapDirectoryUtils.DEFAULT_USER_OBJCLASSES.clone();
        else
            return LdapDirectoryUtils.DEFAULT_AD_USER_OBJCLASSES.clone();
    }

    public static String[] getGroupObjectClasses(Configuration config) {
        if (config.vendor == Vendor.ApacheDS)
            return LdapDirectoryUtils.DEFAULT_GROUP_OBJCLASSES.clone();
        else
            return LdapDirectoryUtils.DEFAULT_AD_GROUP_OBJCLASSES.clone();
    }

    public static String getAttributeById(Configuration config, LdapAttribute attribute) {
        if (config.vendor == Vendor.ApacheDS) {
            return DEFAULT_ATTR_MAP.get(attribute);
        } else {
            return DEFAULT_AD_ATTR_MAP.get(attribute);
        }


    }

}
