package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

public class CursorSubscriptionDto {

    private boolean allEntities;
    private CharSequence[] subscribedEntities;
    private boolean allTypes;
    private String[] subscribedTypes;

    public boolean isAllEntities() {
        return allEntities;
    }

    public void setAllEntities(boolean allEntities) {
        this.allEntities = allEntities;
    }

    public CharSequence[] getSubscribedEntities() {
        return subscribedEntities;
    }

    public void setSubscribedEntities(CharSequence[] subscribedEntities) {
        this.subscribedEntities = subscribedEntities;
    }

    public boolean isAllTypes() {
        return allTypes;
    }

    public void setAllTypes(boolean allTypes) {
        this.allTypes = allTypes;
    }

    public String[] getSubscribedTypes() {
        return subscribedTypes;
    }

    public void setSubscribedTypes(String[] subscribedTypes) {
        this.subscribedTypes = subscribedTypes;
    }
}
