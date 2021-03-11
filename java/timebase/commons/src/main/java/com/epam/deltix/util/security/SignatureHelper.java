package com.epam.deltix.util.security;

import java.security.*;

public class SignatureHelper {
    public static void  update (Signature sig, int num) 
        throws SignatureException 
    {
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
    }

    public static void  update (Signature sig, long num) 
        throws SignatureException 
    {
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
        num >>= 8;
        sig.update ((byte) (num & 255));
    }

}
