package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class ModuleIdentityDefinition extends ObjectDefinition {
    public final String             lastUpdated;
    public final String             organization;
    public final String             contactInfo;
    public final Revision []        revisions;

    public ModuleIdentityDefinition (
        long                        location, 
        String                      id,
        String                      lastUpdated, 
        String                      organization,
        String                      contactInfo, 
        String                      description, 
        Revision []                 revisions,
        OIDValue                    value
    ) 
    {
        super (location, id, description, value);
        
        this.lastUpdated = lastUpdated;
        this.organization = organization;
        this.contactInfo = contactInfo;
        this.revisions = revisions;
    }           
}
