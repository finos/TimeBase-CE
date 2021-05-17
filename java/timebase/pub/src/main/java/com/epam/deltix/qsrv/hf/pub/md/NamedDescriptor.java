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
package com.epam.deltix.qsrv.hf.pub.md;

import java.io.DataInputStream;
import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 */
public abstract class NamedDescriptor implements Serializable {
	private static final long serialVersionUID = 1L;
	
    @XmlElement (name = "name") 
    private String                    name;
    
    @XmlElement (name = "title")
    private String                    title;

    @XmlElement (name = "description")
    private String                    description;
    
    protected NamedDescriptor () {    // For JAXB
        name = null;
        title = null;
    }
    
    protected NamedDescriptor (NamedDescriptor template) {
        name = template.name;
        title = template.title;
        description = template.description;
    }
    
    protected NamedDescriptor (String inName, String inTitle) {
        name = inName;
        title = inTitle;
    }

    public String           getName () {
        return name;
    }

    public String           getTitle () {
        return title;
    }

    public String           getDescription() {
        return description;
    }

    public void             setDescription(String description) {
        this.description = description;
    }

    public void             writeTo (DataOutputStream out, int serial)
        throws IOException
    {
        writeNullableString (name, out);
        writeNullableString (title, out);
        writeNullableString (description, out);
    }

    protected void                  readFields (
        DataInputStream                 in,
        int                             serial
    )
        throws IOException
    {
        name = readNullableString (in);
        title = readNullableString (in);
        description = readNullableString (in);
    }
}
