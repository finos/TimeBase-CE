package com.epam.deltix.qsrv.hf.topic.consumer;

import com.epam.deltix.util.concurrent.CursorIsClosedException;

/**
 * This exception indicates that cursor or other data source was closed because of detected data loss.
 *
 * @author Alexei Osipov
 */
public class ClosedDueToDataLossException extends CursorIsClosedException {
}
