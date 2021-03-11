package com.epam.deltix.qsrv.hf.security.rules;

import com.epam.deltix.util.security.AccessControlRule;

public final class AccessControlEntry {
    private final String currentUser;
    private final String permission;
    private final String targetUser;
    private final AccessControlRule.RuleEffect ruleEffect;

    public AccessControlEntry(AccessControlRule.RuleEffect ruleEffect, String currentUser, String permission, String targetUser) {
        this.ruleEffect = ruleEffect;
        this.currentUser = currentUser;
        this.permission = permission;
        this.targetUser = targetUser;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public String getPermission() {
        return permission;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public AccessControlRule.RuleEffect getRuleEffect() {
        return ruleEffect;
    }

    @Override
    public String toString() {
        return "AccessControlEntry{" +
                "ruleEffect=" + ruleEffect +
                ",currentUser='" + currentUser + '\'' +
                ", permission='" + permission + '\'' +
                ", targetUser='" + targetUser + '\'' +
                '}';
    }
}
