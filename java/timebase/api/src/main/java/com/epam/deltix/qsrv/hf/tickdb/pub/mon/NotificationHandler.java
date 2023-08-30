/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.pub.mon;

import com.epam.deltix.util.time.GlobalTimer;

import java.util.HashSet;
import java.util.TimerTask;

/**
 *
 */
public class NotificationHandler implements PropertyMonitorHandler {

    private String                      id;
    private int                         notificationDelay;

    private final HashSet<PropertyMonitor>   listeners = new HashSet<PropertyMonitor>();
    private PropertyMonitor[]                listenersSnapshot = {};

    private Enum[]                                  properties;
    private Object[]                                values;
    private DelayedTask[]                           tasks;

    class DelayedTask extends TimerTask {
        int index;

        DelayedTask(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            firePropertyChange(index, values[index]);
            tasks[index] = null;
        }
    }

    public NotificationHandler(String id, Enum[] names, int delay) {
        this.id = id;
        this.notificationDelay = delay;

        setProperties(names);
    }
    
    public void setProperties(Enum[] names) {
        this.properties = names;
        this.tasks = new DelayedTask[names.length];
        this.values = new Object[names.length];
    }

    @Override
    public void addPropertyMonitor(PropertyMonitor listener) {
        synchronized (listeners) {
            listeners.add(listener);
            listenersSnapshot = listeners.toArray(new PropertyMonitor[listeners.size()]);
        }
        firePropertiesChange(listener);
    }

    @Override
    public void removePropertyMonitor(PropertyMonitor listener) {
        synchronized (listeners) {
            listeners.remove(listener);
            listenersSnapshot = listeners.toArray(new PropertyMonitor[listeners.size()]);
        }
    }

    public void propertyChanged(Enum name, final Object newValue) {
        int index = name.ordinal();

        values[index] = newValue;

        DelayedTask task = tasks[index];
        if (task == null) {
            task = tasks[index] = new DelayedTask(index);
            GlobalTimer.INSTANCE.schedule(task, notificationDelay);
        }
    }

    private void firePropertiesChange(PropertyMonitor listener) {

        assert properties != null;

        for (int i = 0; i < properties.length; i++) {
            if (values[i] != null)
                listener.propertyChanged(id, properties[i].name(), values[i]);
        }
    }

    public boolean hasListeners() {
        return listenersSnapshot.length > 0;
    }

    private void firePropertyChange(int index, Object newValue) {
        PropertyMonitor[] snapshot = listenersSnapshot;

        for (PropertyMonitor aSnapshot : snapshot)
            aSnapshot.propertyChanged(id, properties[index].name(), newValue);
    }
}