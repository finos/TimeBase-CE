package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

/**
 * @author Alexei Osipov
 */
public enum TopicTransferType {
    IPC(1),
    UDP(2);

    private final byte protocolCode;

    TopicTransferType(int protocolCode) {
        this.protocolCode = (byte) protocolCode;
    }

    public byte getProtocolCode() {
        return protocolCode;
    }

    public static TopicTransferType getByCode(byte protocolCode) {
        for (TopicTransferType type : TopicTransferType.values()) {
            if (type.getProtocolCode() == protocolCode) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TopicTransferType code");
    }
}
