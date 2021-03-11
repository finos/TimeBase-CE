package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.tickdb.pub.DBStateNotifier;
import com.epam.deltix.util.concurrent.QuickExecutor;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *
 */
class DBStateNotifierTask extends QuickExecutor.QuickTask {

    private enum EventType {
        CHANGED,
        ADDED,
        DELETED,
        RENAMED
    }

    private static class EventData {
        protected final String key;
        protected final EventType type;

        protected EventData(final String key, final EventType type) {
            this.key = key;
            this.type = type;
        }

        @Override
        public String toString() {
            return "[" + key + ", " + type + "]";
        }
    }

    private static class RenameEventData extends EventData {
        private final String fromKey;

        public RenameEventData(String fromKey, String toKey) {
            super(toKey, EventType.RENAMED);
            this.fromKey = fromKey;
        }
    }

    private final Queue<EventData> eventsData = new ConcurrentLinkedDeque<>();
    private final DBStateNotifier dbStateNotifier;

    DBStateNotifierTask(final QuickExecutor executor, final DBStateNotifier dbStateNotifier) {
        super(executor);
        this.dbStateNotifier = dbStateNotifier;
    }

    void fireStateRenamed(String fromKey, String toKey) {
        submitRename(fromKey, toKey);
    }

    void fireStateChanged(String key) {
        submit(key, DBStateNotifierTask.EventType.CHANGED);
    }

    void fireAdded(String key) {
        submit(key, DBStateNotifierTask.EventType.ADDED);
    }

    void fireDeleted(String key) {
        submit(key, DBStateNotifierTask.EventType.DELETED);
    }

    private void submit(final String key, final DBStateNotifierTask.EventType type) {
        if (key == null)
            return;

        eventsData.add(new EventData(key, type));
        submit();
    }

    private void submitRename(final String fromKey, final String toKey) {
        if (fromKey == null || toKey == null)
            return;

        eventsData.add(new RenameEventData(fromKey, toKey));
        submit();
    }

    @Override
    public void run() throws InterruptedException {
        for (;;) {
            EventData eventData = eventsData.poll();
            if (eventData == null)
                break;

            notifyChecked(eventData);
        }
    }

    private void notifyChecked(final EventData eventData) {
        try {
            switch (eventData.type) {
                case CHANGED:
                    dbStateNotifier.fireStateChanged(eventData.key);
                    break;
                case ADDED:
                    dbStateNotifier.fireAdded(eventData.key);
                    break;
                case DELETED:
                    dbStateNotifier.fireDeleted(eventData.key);
                    break;
                case RENAMED: {
                    RenameEventData renameEventData = (RenameEventData) eventData;
                    dbStateNotifier.fireRenamed(renameEventData.fromKey, renameEventData.key);
                    break;
                }
            }
        } catch (Throwable t) {
            TickDBClient.LOGGER.warn("Error fire event " + eventData + ": %s").with(t);
        }
    }
}
