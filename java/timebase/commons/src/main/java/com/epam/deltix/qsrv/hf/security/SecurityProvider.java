package com.epam.deltix.qsrv.hf.security;

import java.lang.*;
import java.security.Principal;

public interface SecurityProvider {

    String setCurrentUser(String user);
    String getCurrentUser();

    Principal setCurrentPrincipal(Principal user);
    Principal getCurrentPrincipal();

    SecurityManager getSecurityManager();
}
