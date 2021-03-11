package com.epam.deltix.util.vsocket;

/**
 * @author Alexei Osipov
 */
class VSocketRecoveryInfo {
    private final VSocket socket;

    private int reconnectAttempts = 0;
    private long lastReconnectAttemptTs = Long.MIN_VALUE;
    private final long disconnectTs;

    private boolean recoveryFailed;
    private boolean recoverySucceeded;

    private boolean recoveryAttemptInProgress;

    VSocketRecoveryInfo(VSocket socket, long disconnectTimestamp) {
        this.socket = socket;
        this.disconnectTs = disconnectTimestamp;
    }

    int addReconnectAttempt(long reconnectAttemptTimestamp) {
        reconnectAttempts++;
        lastReconnectAttemptTs = reconnectAttemptTimestamp;
        return reconnectAttempts;
    }

    int getReconnectAttempts() {
        return reconnectAttempts;
    }

    long getLastReconnectAttemptTs() {
        return lastReconnectAttemptTs;
    }

    long getDisconnectTs() {
        return disconnectTs;
    }

    VSocket getSocket() {
        return socket;
    }

    void markRecoveryFailed() {
        recoveryFailed = true;
    }

    void markRecoverySucceeded() {
        recoverySucceeded = true;
    }

    boolean isRecoveryFailed() {
        return recoveryFailed;
    }

    boolean isRecoverySucceeded() {
        return recoverySucceeded;
    }

    boolean isRecoveryEnded() {
        return recoverySucceeded || recoveryFailed;
    }

    boolean isWaitingForRecovery() {
        return recoveryAttemptInProgress || !isRecoveryEnded();
    }

    boolean startRecoveryAttempt() {
        //noinspection RedundantIfStatement
        if (recoveryAttemptInProgress || isRecoveryEnded()) {
            // Only one attempt at a time
            return false;
        } else {
            recoveryAttemptInProgress = true;
            return true;
        }
    }

    void stopRecoveryAttempt() {
        //noinspection RedundantIfStatement
        if (recoveryAttemptInProgress) {
            // Only one attempt at a time
            recoveryAttemptInProgress = false;
        } else {
            throw new IllegalStateException("Attempt to stop recovery multiple times");
        }
    }

    public boolean isRecoveryAttemptInProgress() {
        return recoveryAttemptInProgress;
    }
}
