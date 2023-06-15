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
package com.epam.deltix.util.ldap.security;

import com.epam.deltix.qsrv.hf.security.LDAPTools;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.lang.Factory;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.ldap.LDAPConnection;
import com.epam.deltix.util.ldap.LDAPContext;
import com.epam.deltix.util.ldap.config.Query;
import com.epam.deltix.util.security.*;
import com.epam.deltix.util.text.IgnoreCaseComparator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.File;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LdapUserDirectoryFactory implements Factory<AuthenticatingUserDirectory> {
    private final File configFile;

    public LdapUserDirectoryFactory(File configFile) {
        this.configFile = configFile;
    }

    @Override
    public AuthenticatingUserDirectory create() {
        return LdapUserDirectory.create(configFile);
    }
}

class LdapUserDirectory implements AuthenticatingUserDirectory {
    private static final Logger LOGGER = LDAPConnection.LOGGER;
    private static final String LOGPREFIX = LDAPConnection.LOGPREFIX;

    private final TreeMap<String, LdapGroupEntry> groups = new TreeMap<>(IgnoreCaseComparator.INSTANCE);
    private final TreeMap<String, LdapUserEntry> users = new TreeMap<>(IgnoreCaseComparator.INSTANCE);
    private final HashMap<LdapUserEntry, HashSet<LdapGroupEntry>> userGroups = new HashMap<>();

    private final LDAPContext context;

    LdapUserDirectory (LDAPContext context) {
        this.context = context;
    }

    static EntryFactory createFactory(Configuration config) {
        EntryFactory factory = new EntryFactory();
        factory.addBinding(LdapUserEntry.class, config.user);
        factory.addBinding(LdapGroupEntry.class, config.group);

        return factory;
    }

    static LdapUserDirectory create(File configFile) {
        try {
            return create(Configuration.read(configFile));
        } catch (Exception e) {
            throw new UncheckedIOException("Error loading user directory from LDAP: " + e.getMessage(), e);
        }
    }

    public static LdapUserDirectory create(Configuration config) throws Exception {
        DirContext dirContext;
        if (config.credentials != null)
            dirContext = LDAPConnection.connect(config.connection, config.credentials.name, config.credentials.get());
        else
            dirContext = LDAPConnection.connect(config.connection);

        LdapUserDirectory result = new LdapUserDirectory(new LDAPContext(dirContext, config));
        result.load(config);
        return result;
    }

    /////////////////////// UAC INTERFACES IMPL //////////////////////

    @Override
    public Principal[] users() {
        return users.values().toArray(new Principal[users.size()]);
    }

    @Override
    public Principal getUser(String name) {
        return users.get(name);
    }

    @Override
    public Principal[] groups() {
        return groups.values().toArray(new Principal[groups.size()]);
    }

    @Override
    public Principal getGroup(String name) {
        return groups.get(name);
    }

    @Override
    public synchronized Principal authenticate(String name, String pass) {
        LdapUserEntry entry = findUser(name);

        if (entry != null && LDAPConnection.authenticate(context, entry.distinguishedName, pass))
            return entry;

        throw new AccessControlException("Not authorized.");
    }

    private LdapUserEntry   findUser(String name) {
        LdapUserEntry user = users.get(name);
        if (user != null)
            return user;

        for (Map.Entry<String, LdapUserEntry> e : users.entrySet()) {
            user = e.getValue();
            if (name.equalsIgnoreCase(user.distinguishedName) || name.equalsIgnoreCase(user.id))
                return user;
        }

        return null;
    }

    @Override
    public void changePassword(String user, String oldPassword, String newPassword) {
        LdapUserEntry entry = users.get(user);
        if (entry == null)
            throw new AccessControlException("User '" + user + "' is unknown!");

        LDAPConnection.changePassword(context, context.config.credentials.name, context.config.credentials.get(),
                                      entry.distinguishedName, oldPassword, newPassword);
    }

    //////////////////////////// LOADING UTILS //////////////////////////

    @SuppressFBWarnings(value = "LDAP_INJECTION",justification = "Sanitizing injectable parameters")
    private void addGroups(String filter, EntryFactory factory, String... nodes) throws Exception {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        //controls.setReturningAttributes(factory.getAttributes(LdapGroupEntry.class));

        for (String node : nodes) {
            NamingEnumeration<SearchResult> results = context.context.search(node, filter, controls);

            while (results.hasMore()) {
                Attributes groupAttributes = results.next().getAttributes();
                if (groupAttributes == null)
                    continue;

                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(Level.FINE, LOGPREFIX + "Group attributes: {0}", groupAttributes);

                LdapGroupEntry group = (LdapGroupEntry) factory.create(LdapGroupEntry.class, groupAttributes);
                groupFound(group);
            }
        }
    }

    private synchronized void load(Configuration config) throws Exception {
        EntryFactory factory = createFactory(config);

        if (config.groups != null) {
            for (Query q : config.groups)
                addGroups(q.filter, factory, q.node);
        }
		
		for (LdapGroupEntry groupEntry : groups.values())
            processGroupEntry(groupEntry, factory);

        for (LdapGroupEntry groupEntry : groups.values()) {
            HashSet<LdapGroupEntry> chain = new HashSet<>();
            buildChain(groupEntry, chain);
        }
		
        if (LOGGER.isLoggable(Level.FINE)) {
            StringBuilder builder = new StringBuilder(512);
            builder.append(LOGPREFIX).append("Loaded LDAP UAC configuration:\n");
            builder.append("USERS:\n");
            for (UserEntry user : users.values())
                builder.append("\t").append(user.getName()).append("\n");

            builder.append("GROUPS:\n");
            for (LdapGroupEntry group : groups.values()) {
                builder.append("\t").append(group.getName()).append("\n");
                builder.append("\t\tMembers: ").append(Arrays.toString(group.getMembers())).append('\n');
            }

            LOGGER.log(Level.FINE, builder.toString());
        }
    }

    private void buildChain(LdapGroupEntry group, HashSet<LdapGroupEntry> chain) {
        if (chain.contains(group))
            return;

        chain.add(group);

        for (Principal principal : group.getPrincipals()) {
            if (principal instanceof LdapGroupEntry) {
                LdapGroupEntry entry = (LdapGroupEntry) principal;
                buildChain(entry, chain);
            } else if (principal instanceof LdapUserEntry) {
                HashSet<LdapGroupEntry> set = userGroups.get(principal);
                if (set == null)
                    userGroups.put((LdapUserEntry) principal, set = new HashSet<>());
                set.addAll(chain);
            }
        }

        chain.remove(group);
    }

    private void processGroupEntry(LdapGroupEntry group, EntryFactory factory) throws Exception {
        if (!group.hasMembers())
            return;

        for (String member : group.getMembers()) {
            final Attributes attributes;
            try {
                attributes = context.context.getAttributes(member);
            } catch (NameNotFoundException e) {
                LOGGER.warning(LOGPREFIX + "Group member is not found: " + member);
                continue;
            }

            Entry entry = factory.create(attributes);
            if (entry instanceof PrincipalEntry) {

                // ApacheDS does not return entryDN attribute in getAttributes
                if (entry instanceof LdapUserEntry) {
                    LdapUserEntry userEntry = (LdapUserEntry) entry;
                    if (userEntry.distinguishedName == null)
                        userEntry.distinguishedName = member;

                    userFound(userEntry);
                } else if (entry instanceof LdapGroupEntry) {
                    LdapGroupEntry groupEntry = (LdapGroupEntry) entry;
                    if (groupEntry.distinguishedName == null)
                        groupEntry.distinguishedName = member;
                }

                PrincipalEntry principal = getEntry((PrincipalEntry) entry);
                if (principal != null) {
                    group.addPrincipal(principal);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE,LOGPREFIX + "User {0} added to group {1}", new Object[] {principal.getName(), group.getName()});
                    }
                } else
                    LOGGER.warning(LOGPREFIX + "Unknown group member: " + member);
            }
        }
    }

    private PrincipalEntry getEntry(PrincipalEntry entry) {
        if (entry instanceof UserEntry)
            return users.get(entry.getName());
        else if (entry instanceof GroupEntry)
            return groups.get(entry.getName());

        throw new IllegalArgumentException("Entry " + entry + " is not supported");
    }

    private boolean groupFound(LdapGroupEntry group) {
        String name = group.getName();
        if (!groups.containsKey(name) && !users.containsKey(name)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Group added: {0}", name);
            }
            groups.put(name, group);
            return true;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Group not added: {0}. Reason: duplicate group or user", name);
        }
        return false;
    }

    private boolean userFound(LdapUserEntry user) {
        String name = user.getName();
        if (!users.containsKey(name) && !groups.containsKey(name)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "User added: {0}", name);
            }
            users.put(name, user);
            return true;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "User not added: {0}. Reason: duplicate group or user", name);
        }
        return false;
    }

    //////////////////////// HELPER CLASSES /////////////////////

    static final class LdapGroupEntry extends GroupEntry {
        public String[] members;
        public String distinguishedName;

        public LdapGroupEntry(String id) {
            super(id);
        }

        public String[] getMembers() {
            return members != null && members.length > 0 ? members.clone() : Util.EMPTY_STRING_ARRAY;
        }

        public boolean hasMembers() {
            return members != null && members.length > 0;
        }

        public void setMembers(String[] members) {
            this.members = members;
        }
    }

    static final class LdapUserEntry extends UserEntry {
        public String distinguishedName;

        public LdapUserEntry(String id) {
            super(id);
        }
    }
}