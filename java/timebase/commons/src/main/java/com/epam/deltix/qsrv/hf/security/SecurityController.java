package com.epam.deltix.qsrv.hf.security;

import com.epam.deltix.qsrv.hf.security.rules.AccessControlEntry;

import java.util.List;

public interface SecurityController {
    void reloadPermissions();

    List<AccessControlEntry> getEffectivePermissions();
}
