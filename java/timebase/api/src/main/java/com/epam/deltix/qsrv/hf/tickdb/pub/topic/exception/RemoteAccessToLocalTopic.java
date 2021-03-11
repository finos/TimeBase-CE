package com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception;

/**
 * Thrown in case of attempt to access an IPC-only topic from remote machine.
 * Note: topic must be created as multicast topic if you want to access it from other machines.
 *
 * Note: this class does not implement {@link TopicApiException} because this exception is severe and it is not a part of API.
 *
 * @author Alexei Osipov
 */
public class RemoteAccessToLocalTopic extends RuntimeException {
    public RemoteAccessToLocalTopic() {
        super("Attempt to access topic on remote TB via IPC");
    }

    public RemoteAccessToLocalTopic(String message) {
        super(message);
    }
}
