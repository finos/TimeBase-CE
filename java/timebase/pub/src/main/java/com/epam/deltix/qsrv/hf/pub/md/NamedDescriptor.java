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
