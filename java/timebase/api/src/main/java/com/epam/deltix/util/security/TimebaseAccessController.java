package com.epam.deltix.util.security;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.util.lang.Disposable;

import java.security.Principal;

/**
 * Created by Alex Karpovich on 04/11/2019.
 */
public interface TimebaseAccessController  extends Disposable {

    //boolean                     connected(Principal user, String address);

    DataFilter<RawMessage> createFilter(Principal user, String address);

    //void                        disconnected(Principal user, String address);
}
