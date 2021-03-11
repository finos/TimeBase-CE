package com.epam.deltix.util.io.aeron;

/**
 * This exception indicates that {@link deltix.data.stream.MessageChannel} (or other similar data receiver)
 * was closed and can't longer be used. Any further attempts to use this channel will fail.
 *
 * If you need to continue operation then you should re-create the channel.
 *
 * @author Alexei Osipov
 */
public class PublicationClosedException extends RuntimeException {
}
