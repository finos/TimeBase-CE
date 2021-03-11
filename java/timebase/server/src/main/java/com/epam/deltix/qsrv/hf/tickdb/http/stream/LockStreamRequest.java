package com.epam.deltix.qsrv.hf.tickdb.http.stream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "lockStream")
public class LockStreamRequest extends StreamRequest {
    /*
        Lock type. If true - request for "WRITE" lock, otherwise "READ".
     */
    @XmlElement(name = "write")
    public boolean      write;

    /*
        if timeout > 0, the server "tryLock" operation will be used.
     */
    @XmlElement(name = "timeout")
    public long         timeout;

    /*
     *  Session identifier
     */
    @XmlElement(name = "sid")
    public String        sid;
}
