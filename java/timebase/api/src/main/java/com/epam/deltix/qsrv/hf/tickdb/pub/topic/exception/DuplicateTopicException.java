package com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception;

/**
 * Thrown in case of attempt to create topic if topic with such key already exists.
 *
 * @author Alexei Osipov
 */
public class DuplicateTopicException extends RuntimeException implements TopicApiException {

    public DuplicateTopicException(String message) {
        super(message);
    }
}
