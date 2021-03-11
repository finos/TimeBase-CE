package com.epam.deltix.qsrv.hf.codec.cg;


public class CodecCacheKey {

    private final String targetClassName;
    private final String guid;
    private final String type;


    public CodecCacheKey(String targetClassName, String guid, String type) {
        this.targetClassName = targetClassName;
        this.guid = guid;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CodecCacheKey key = (CodecCacheKey) o;

        if (!getTargetClassName().equals(key.getTargetClassName())) return false;
        if (!getGuid().equals(key.getGuid())) return false;
        return getType().equals(key.getType());

    }

    @Override
    public int hashCode() {
        int result = getTargetClassName().hashCode();
        result = 31 * result + getGuid().hashCode();
        result = 31 * result + getType().hashCode();
        return result;
    }

    public String getTargetClassName() {
        return targetClassName;
    }


    public String getGuid() {
        return guid;
    }


    public String getType() {
        return type;
    }

}
