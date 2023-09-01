/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.security.repository;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.security.SecurityControllerFactory;
import com.epam.deltix.qsrv.hf.security.repository.impl.LdapUserDirectory;
import com.epam.deltix.qsrv.util.cmd.AbstractShellEx;
import com.epam.deltix.util.collections.CollectionUtil;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.ldap.LDAPConnection.Vendor;
import com.epam.deltix.util.ldap.security.Configuration;

/**
 * Description: UserDirectoryShell
 * Date: Feb 28, 2011
 *
 * @author Nickolay Dul
 */
public class UserDirectoryShell extends AbstractShellEx {


    public static final String QS_SECURITY = "QuantServer.security";

    public static final Logger LOGGER = Logger.getLogger(UserDirectoryShell.class.getName());
    private Vendor repositoryType = Vendor.ApacheDS;

    private UserDirectory repository = null;

    public UserDirectoryShell(String[] args) {
        super(args);
    }

    public static void main(String[] args) throws Throwable {
        new UserDirectoryShell(args).run();
    }

    @Override
    protected void doSet() {

        outWriter.println("Repository type:   " + repositoryType);

        if (repository != null) {
            outWriter.println("usernode:        " + repository.getUserNode());
            outWriter.println("groupnode:       " + repository.getGroupNode());
        } else {
            outWriter.println("usernode:        Not specified");
            outWriter.println("groupnode:       Not specified");
        }

        super.doSet();
    }

    @Override
    protected boolean doSet(String option, String value) throws Exception {

        if (option.equalsIgnoreCase("usernode")) {
            assertRepositoryConnected();
            repository.setUserNode(StringUtils.trim(value));
            confirm("User node:   " + repository.getUserNode());
            return true;
        }
        return super.doSet(option, value);
    }

    @Override
    protected boolean doCommand(String key, String args) throws Exception {

        if (super.doCommand(key, args)) {
            return true;
        }

        if (key.equalsIgnoreCase("connect")) {
            try {
                QSHome.get();
            } catch (Exception exc) {
                LOGGER.log(Level.INFO, "QuantServer Home is not specified. Please specify it before connecting.");
                return true;
            }
            connectLdap();
            confirm("Connected!");
            return true;
        } else if (key.equalsIgnoreCase("disconnect")) {
            Util.close(repository);
            repository = null;
            confirm("Disconnected!");
            return true;
        } else if (key.equalsIgnoreCase("init")) {
            assertRepositoryConnected();
            if (repository instanceof LdapUserDirectory)
                ((LdapUserDirectory) repository).initialize();
            return true;
        } else if (key.equalsIgnoreCase("list")) {
            assertRepositoryConnected();
            System.out.println(usersToString(repository.listUsers()));
            System.out.println(groupsToString(repository.listGroups()));
            return true;
        } else if (key.equalsIgnoreCase("listusers")) {
            assertRepositoryConnected();
            System.out.println(usersToString(repository.listUsers()));
            return true;
        } else if (key.equalsIgnoreCase("listgroups")) {
            assertRepositoryConnected();
            System.out.println(groupsToString(repository.listGroups()));
            return true;
        } else if (key.equalsIgnoreCase("getuser")) {
            assertRepositoryConnected();
            UserEntity user = repository.getUserById(args);
            if(user == null) {
                LOGGER.severe("User [" + args + "] is not found!");
                return true;
            }
            System.out.println(usersToString(Collections.singleton(user)));
            return true;
        } else if (key.equalsIgnoreCase("newuser")) {
            assertRepositoryConnected();
            if (repositoryType == Vendor.ActiveDirectory) {
                LOGGER.log(Level.INFO, "Adding new users for Microsoft Active Directory is not supported");
            } else {
                UserEntity user = parseUserEntity(args);
                repository.createUser(user);
                confirm("New user node is created");
            }
            return true;
        } else if (key.equalsIgnoreCase("deleteuser")) {
            assertRepositoryConnected();
            if (repositoryType == Vendor.ActiveDirectory) {
                LOGGER.log(Level.INFO, "Deleting users for Microsoft Active Directory is not supported");
            } else {
                repository.deleteUser(args);
                confirm("User node is removed");
            }
            return true;
        } else if (key.equalsIgnoreCase("modifyuser")) {
            assertRepositoryConnected();
            if (repositoryType == Vendor.ActiveDirectory) {
                LOGGER.log(Level.INFO, "Modifying users for Microsoft Active Directory is not supported");
            } else {
                UserEntity user = parseUserEntity(args);
                repository.modifyUser(user);
                confirm("User node is modified");
            }
            return true;
        } else if (key.equalsIgnoreCase("getgroup")) {
            assertRepositoryConnected();
            GroupEntity group = repository.getGroupById(args);
            if(group == null) {
                LOGGER.severe("Group [" + args + "] is not found!");
                return true;
            }
            System.out.println(groupsToString(Collections.singleton(group)));
            return true;
        } else if (key.equalsIgnoreCase("newgroup")) {
            assertRepositoryConnected();
            GroupEntity group = parseGroupEntity(args);
            repository.createGroup(group);
            confirm("New group node is created");
            return true;
        } else if (key.equalsIgnoreCase("deletegroup")) {
            assertRepositoryConnected();
            repository.deleteGroup(args);
            confirm("Group node is removed");
            return true;
        } else if (key.equalsIgnoreCase("modifygroup")) {
            assertRepositoryConnected();
            GroupEntity group = parseGroupEntity(args);
            repository.modifyGroup(group);
            confirm("Group node is modified");
            return true;
        } else if (key.equalsIgnoreCase("addgroupmembers")) {
            assertRepositoryConnected();
            GroupEntity group = parseGroupEntity(args);
            repository.addGroupMembers(group.getId(), group.getMembers());
            confirm("New group members are added");
            return true;
        } else if (key.equalsIgnoreCase("removegroupmembers")) {
            assertRepositoryConnected();
            GroupEntity group = parseGroupEntity(args);
            repository.removeGroupMembers(group.getId(), group.getMembers());
            confirm("Group members are removed");
            return true;
        }

        return false;
    }

    private GroupEntity parseGroupEntity(String args) {
        String[] values = StringUtils.split(args, ";", true, false);

        Set<String> members = null;
        if (values.length > 1 && values[1] != null)
            members = new HashSet<>(Arrays.asList(StringUtils.split(values[1], ",", true, true)));

        String description = null;
        if (values.length > 2)
            description = values[2];
        return new GroupEntity(values[0], description, members);
    }

    private static UserEntity parseUserEntity(String args) {
        String[] values = StringUtils.split(args, ";", true, false);
        Set<String> groups = null;
        if (values.length > 2 && values[2] != null)
            groups = new HashSet<>(Arrays.asList(StringUtils.split(values[2], ",", true, true)));
        String firstName = null;
        if (values.length > 3)
            firstName = values[3];
        String lastName = null;
        if (values.length > 4)
            lastName = values[4];
        String description = null;
        if (values.length > 5)
            description = values[5];

        return new UserEntity(values[0], values[1], firstName, lastName, description, groups);
    }

    private void connectLdap() {
        Configuration config;
        Properties adminProperties;
        try {
            adminProperties = IOUtil.readPropsFromFile(new File(QSHome.getFile("config"), "admin.properties"));
        } catch (IOException exc) {
            throw new RuntimeException("admin.properties file can't be found in specified home path!");
        }
        if (adminProperties.getProperty(QS_SECURITY).equals("FILE")) {
            LOGGER.log(Level.INFO, "File Security is not supported by UACShell.");
            return;
        }
        try {

            config = Configuration.read(new File(QSHome.getFile("config"), SecurityControllerFactory.LDAP_SECURITY_FILE_NAME));
        }  catch (JAXBException exc) {
            throw new RuntimeException("Error reading configuration file: " + exc.getMessage());
        }
        repositoryType = config.vendor;

        repository = new LdapUserDirectory(config);


    }

    private String usersToString(Collection<UserEntity> users) {
        StringBuilder builder = new StringBuilder("\n>>> Directory Users:");
        for (UserEntity user : users) {
            builder.append("\n").
                    append(String.format("%-12s", user.getId())).
                    append(user.getFirstName()).append(" ").append(user.getSecondName()).append(", ").
                    append("Member Of: (").append(CollectionUtil.toString(user.getUserGroups(), ", ")).append("), ").
                    append("Description: ").append(user.getDescription());
        }
        return builder.toString();
    }

    private String groupsToString(Collection<GroupEntity> groups) {
        StringBuilder builder = new StringBuilder("\n>>> Directory Groups:");
        for (GroupEntity group : groups) {
            builder.append("\n").
                    append(String.format("%-15s", group.getId())).
                    append("Members: (").append(CollectionUtil.toString(group.getMembers(), ", ")).append(")");
        }
        return builder.toString();
    }

    private void assertRepositoryConnected() {
        if (repository == null)
            throw new RuntimeException("You are not connected. Please, run command \"connect\" first!");
    }

}