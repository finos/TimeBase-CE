package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.tickdb.comm.UserCredentials;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.security.TimeBasePermissions;
import com.epam.deltix.util.security.SecurityController;
import com.epam.deltix.util.security.TimebaseAccessController;

import java.security.Principal;

/**
 *
 */
class SecurityContext {

    final SecurityController controller;
    final TimebaseAccessController ac;

    SecurityContext(SecurityController controller, TimebaseAccessController ac) {
        this.controller = controller;
        this.ac = ac;
    }

    public boolean      isVisible(Principal user, DXTickStream stream) {
        return hasPermission(user, TimeBasePermissions.READ_PERMISSION, stream); // WRITE no longer implies READ
    }

    public void         checkReadable(Principal user, DXTickStream stream) {
        checkPermission(user, TimeBasePermissions.READ_PERMISSION, stream);
    }

    public void         checkWritable(Principal user, DXTickStream stream) {
        checkPermission(user, TimeBasePermissions.WRITE_PERMISSION, stream);
    }

    public Principal    authenticate(UserCredentials c) {
        if (c.delegate != null)
            return controller.impersonate(c.user, c.pass, c.delegate);

        return controller.authenticate(c.user, c.pass);
    }

    private boolean     hasPermission(Principal user, String permission, DXTickStream stream) {
        return controller.hasPermission(user, permission, stream);
    }

    private void        checkPermission(Principal user, String permission, DXTickStream stream) {
        controller.checkPermission(user, permission, stream);
    }

}