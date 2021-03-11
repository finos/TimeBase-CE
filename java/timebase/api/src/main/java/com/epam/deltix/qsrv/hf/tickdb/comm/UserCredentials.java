package com.epam.deltix.qsrv.hf.tickdb.comm;


public class UserCredentials {

    public String   protocol;
    public String   user;
    public String   pass;
    public String   delegate;

    public UserCredentials(String protocol, String user, String pass) {
        this.protocol = protocol;
        this.user = user;
        this.pass = pass;
    }


    public String            getName() {
        return delegate != null ? delegate : user;
    }
}
