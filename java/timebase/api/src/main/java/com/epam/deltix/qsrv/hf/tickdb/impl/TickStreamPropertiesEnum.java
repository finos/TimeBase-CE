package com.epam.deltix.qsrv.hf.tickdb.impl;

import javax.annotation.Nonnull;

/**
 * Enum version of {@link TickStreamProperties}.
 *
 * @author Alexei Osipov
 */
public enum  TickStreamPropertiesEnum {
    KEY(TickStreamProperties.KEY),
    NAME(TickStreamProperties.NAME),
    DESCRIPTION(TickStreamProperties.DESCRIPTION),
    PERIODICITY(TickStreamProperties.PERIODICITY),
    SCHEMA(TickStreamProperties.SCHEMA),
    TIME_RANGE(TickStreamProperties.TIME_RANGE),
    ENTITIES(TickStreamProperties.ENTITIES),
    HIGH_AVAILABILITY(TickStreamProperties.HIGH_AVAILABILITY),
    UNIQUE(TickStreamProperties.UNIQUE),
    BUFFER_OPTIONS(TickStreamProperties.BUFFER_OPTIONS),

    VERSIONING(TickStreamProperties.VERSIONING),
    DATA_VERSION(TickStreamProperties.DATA_VERSION),
    REPLICA_VERSION(TickStreamProperties.REPLICA_VERSION),

    BG_PROCESS(TickStreamProperties.BG_PROCESS),
    WRITER_CREATED(TickStreamProperties.WRITER_CREATED),
    WRITER_CLOSED(TickStreamProperties.WRITER_CLOSED),

    SCOPE(TickStreamProperties.SCOPE),
    DF(TickStreamProperties.DF),

    OWNER(TickStreamProperties.OWNER),

    LOCATION(TickStreamProperties.LOCATION);

    private final int propertyId;

    TickStreamPropertiesEnum(int propertyId) {
        this.propertyId = propertyId;
    }

    public int getPropertyId() {
        return propertyId;
    }

    @Nonnull
    public static TickStreamPropertiesEnum getValueByPropertyId(int propertyId) {
        // Note: we can use switch case here
        for (TickStreamPropertiesEnum val : TickStreamPropertiesEnum.values()) {
            if (val.getPropertyId() == propertyId) {
                return val;
            }
        }
        throw new IllegalArgumentException("No property with id=" + propertyId);
    }
}
