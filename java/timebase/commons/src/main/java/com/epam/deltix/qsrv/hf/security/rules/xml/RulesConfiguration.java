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
package com.epam.deltix.qsrv.hf.security.rules.xml;

import com.epam.deltix.util.lang.Depends;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.xml.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.ArrayList;

@Depends({"../jaxb.index"})
@XmlRootElement(name = "rules")
public class RulesConfiguration {

    private static JAXBContext CONTEXT;
    static {
        try {
            String path = StringUtils.join(":",
                    RulesConfiguration.class.getPackage().getName(),
                    Rule.class.getPackage().getName()
            );

            CONTEXT = JAXBContextFactory.newInstance(path);
        } catch (JAXBException x) {
            Util.logException("Failed to initialize JAXB context for access rules configuration:%s", x);
            throw new ExceptionInInitializerError (x);
        }
    }


    @XmlElements({
            @XmlElement(name = "allow", type = AllowRule.class),
            @XmlElement(name = "deny", type = DenyRule.class)
    })
    public ArrayList<Rule> rules;

    public static RulesConfiguration read(File file) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContextFactory.createStdUnmarshaller(CONTEXT);
        return (RulesConfiguration) unmarshaller.unmarshal(file);
    }

    public static void write(RulesConfiguration config, File file) throws JAXBException {
        Marshaller marshaller = JAXBContextFactory.createStdMarshaller(CONTEXT);
        marshaller.marshal(config, file);
    }
}