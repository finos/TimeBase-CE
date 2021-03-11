package com.epam.deltix.qsrv.hf.security.repository;

import com.epam.deltix.util.id.Identifiable;
import com.epam.deltix.util.lang.StringUtils;

/**
 * Description: SecurityEntity
 * Date: Mar 2, 2011
 *
 * @author Nickolay Dul
 */
public abstract class SecurityEntity implements Identifiable<String> {
    protected final String entityId;
    protected String description;

    protected SecurityEntity(String entityId, String description) {
        this.entityId = StringUtils.trim(entityId);
        if (this.entityId == null)
            throw new IllegalStateException("Security entity id could not be empty!");

        this.description = description;
    }

    @Override
    public String getId() {
        return entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
