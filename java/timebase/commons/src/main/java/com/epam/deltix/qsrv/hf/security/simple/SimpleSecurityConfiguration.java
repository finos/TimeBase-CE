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
package com.epam.deltix.qsrv.hf.security.simple;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.ArrayList;

import com.epam.deltix.util.xml.JAXBContextFactory;

@XmlRootElement(name = "config")
public class SimpleSecurityConfiguration {

    public SimpleSecurityConfiguration() {}

    @XmlElement(name = "users")
    public Users users;

    @XmlElement(name = "groups")
    public Groups groups;

    //Helper Classes

    public static class Users {
        @XmlElement(name = "user", required = true)
        public ArrayList<User> users;
    }

    public static class Groups {
        @XmlElement(name = "group", required = true)
        public ArrayList<Group> groups;
    }

    public static SimpleSecurityConfiguration read(File file) {
        try {
            JAXBContext jaxbContext = JAXBContextFactory.newInstance(SimpleSecurityConfiguration.class.getPackage().getName());
            Unmarshaller unmarshaller = JAXBContextFactory.createStdUnmarshaller(jaxbContext);
            return (SimpleSecurityConfiguration) unmarshaller.unmarshal(file);
        } catch (JAXBException exc) {
            throw new RuntimeException("Principals could not be read from file: " + file, exc);
        }
    }

    public static void write(SimpleSecurityConfiguration permissions, String path) {
        try {
            JAXBContext jaxbContext = JAXBContextFactory.newInstance(SimpleSecurityConfiguration.class.getPackage().getName());
            Marshaller marshaller = JAXBContextFactory.createStdMarshaller(jaxbContext);
            marshaller.marshal(permissions, new File(path));
        } catch (JAXBException e) {
            throw new RuntimeException("Principals could not be saved to file: " + path, e);
        }
    }

    public static class Group {
        public Group() {}

        public Group(String name, ArrayList<String> principal) {
            this.name = name;
            this.principal = principal;
        }

        @XmlAttribute(name = "id", required = true)
        public String name;

        @XmlElement(name = "principal")
        public ArrayList<String> principal;
    }

    public static class User {
        public User() {}

        public User(String id, String password) {
            this.id = id;
            this.password = password;
        }

        @XmlAttribute(name = "id", required = true)
        public String id;

        @XmlElement(name = "password", required = true)
        public String password;
    }

}
