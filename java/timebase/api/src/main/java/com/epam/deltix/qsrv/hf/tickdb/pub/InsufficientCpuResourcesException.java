package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 * Indicates that server was unable to allocate sufficient CPU resources for requested operation.
 * This exception may occur when you attempt to create {@link deltix.qsrv.hf.pub.ChannelPerformance#LATENCY_CRITICAL}
 * cursor or loader and server does not have sufficient resources for that.
 * <p>
 *
 * In case of this exception you may try to repeat the operation. However this operation is expected to succeed only if
 * some resources were freed up by closing other LATENCY_CRITICAL cursor or loader.
 */
public class InsufficientCpuResourcesException extends RuntimeException {
    public InsufficientCpuResourcesException() {
    }
}
