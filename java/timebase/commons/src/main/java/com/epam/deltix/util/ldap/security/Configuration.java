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
package com.epam.deltix.util.ldap.security;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.lang.Depends;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.ldap.LDAPConnection.Vendor;
import com.epam.deltix.util.ldap.config.Binding;
import com.epam.deltix.util.ldap.config.Credentials;
import com.epam.deltix.util.ldap.config.Query;
import com.epam.deltix.util.xml.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.ArrayList;

@Depends({"../jaxb.index", "../../config/jaxb.index"})
@XmlRootElement(name = "config")
public class Configuration {
    private static final Log LOG = LogFactory.getLog(Configuration.class);
    public static JAXBContext CTX;
    static {
        try {
            String path = StringUtils.join(":", Configuration.class.getPackage().getName(), Query.class.getPackage().getName());
            CTX = JAXBContextFactory.newInstance(path);
        } catch (JAXBException x) {
            LOG.error("Failed to initialize JAXB context for security configuration: %s").with(x);
            throw new ExceptionInInitializerError(x);
        }
    }

    public Configuration() {
    } // JAXB

    @XmlElement(name = "vendor")
    public Vendor vendor = Vendor.ApacheDS;

    @XmlElement(name = "connection", required = true)
    public ArrayList<String> connection;

    @XmlElement(name = "credentials")
    public Credentials credentials;

    @XmlElement(name = "groups", required = true)
    public ArrayList<Query> groups;

    @XmlElement(name = "user", required = true)
    public Binding user;

    @XmlElement(name = "group", required = true)
    public Binding group;

    public static Configuration read(File file) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContextFactory.createStdUnmarshaller(CTX);
        return (Configuration) unmarshaller.unmarshal(file);
    }

    public static void write(Configuration config, File file) throws JAXBException {
        Marshaller marshaller = JAXBContextFactory.createStdMarshaller(CTX);
        marshaller.marshal(config, file);
    }
}