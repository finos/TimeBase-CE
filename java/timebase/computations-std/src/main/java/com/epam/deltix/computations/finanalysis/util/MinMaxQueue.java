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

package com.epam.deltix.computations.finanalysis.util;

import java.util.Arrays;

public class MinMaxQueue {
    final int defaultCapacity = 64;

    double[] queueBuffer;
    long[] dateTimeBuffer;
    int[] pointsBuffer;

    int count = 0;
    int requestCompleted = 0;
    int currentPoint = 0;
    int firstIndex = 0;
    int lastIndex = 0;
    int capacity;
    int period;
    long timePeriod;
    boolean isTimeManaged;
    MODE mode;

    public MinMaxQueue(int capacity, MODE mode) {
        this.mode = mode;
        this.capacity = capacity;
        this.period = capacity;
        isTimeManaged = false;

        queueBuffer = new double[this.capacity];
        dateTimeBuffer = new long[this.capacity];
        pointsBuffer = new int[this.capacity];
    }

    public MinMaxQueue(long timePeriod, MODE mode) {
        this.mode = mode;
        this.capacity = defaultCapacity;
        this.timePeriod = timePeriod;
        isTimeManaged = true;

        queueBuffer = new double[this.capacity];
        dateTimeBuffer = new long[this.capacity];
        pointsBuffer = new int[this.capacity];
    }

    public void copyTo(Object target) {
        MinMaxQueue destination = (MinMaxQueue) target;

        destination.capacity = this.capacity;
        destination.lastIndex = this.lastIndex;
        destination.firstIndex = this.firstIndex;
        destination.count = this.count;

        destination.period = this.period;
        destination.timePeriod = this.timePeriod;
        destination.isTimeManaged = this.isTimeManaged;

        destination.requestCompleted = this.requestCompleted;
        destination.currentPoint = this.currentPoint;

        destination.mode = this.mode;

        destination.queueBuffer = this.queueBuffer.clone();
        destination.dateTimeBuffer = this.dateTimeBuffer.clone();
        destination.pointsBuffer = this.pointsBuffer.clone();

        for (int i = 0; i < queueBuffer.length; ++i)
            destination.queueBuffer[i] = this.queueBuffer[i];

        for (int i = 0; i < dateTimeBuffer.length; ++i)
            destination.dateTimeBuffer[i] = this.dateTimeBuffer[i];

        for (int i = 0; i < pointsBuffer.length; ++i)
            destination.pointsBuffer[i] = this.pointsBuffer[i];
    }

    public boolean put(double value, int point) {
        if (count > capacity)
            return false;

        while (count > 0) {
            if (pointsBuffer[firstIndex] + period > point)
                break;

            count--;
            firstIndex = firstIndex == capacity - 1 ? 0 : firstIndex + 1;
        }

        if (mode == MODE.MINIMUM) {
            while (count > 0) {
                int newLast;

                if ((newLast = lastIndex - 1) == -1)
                    newLast = capacity - 1;

                if (queueBuffer[newLast] > value) {
                    lastIndex = newLast;
                    count--;
                } else
                    break;
            }
        } else {
            while (count > 0) {
                int newLast;

                if ((newLast = lastIndex - 1) == -1)
                    newLast = capacity - 1;

                if (queueBuffer[newLast] < value) {
                    lastIndex = newLast;
                    count--;
                } else
                    break;
            }
        }
        // store new element and increase count
        queueBuffer[lastIndex] = value;
        pointsBuffer[lastIndex] = point;
        count++;

        lastIndex = lastIndex == capacity - 1 ? 0 : lastIndex + 1;

        return true;
    }

    public boolean put(double value, long timestamp) {
        if (isTimeManaged) {
            if (count > 0 && timestamp < dateTimeBuffer[lastIndex == 0 ? capacity - 1 : lastIndex - 1])
                return false;

            while (count > 0) {
                if (dateTimeBuffer[firstIndex] + timePeriod >= timestamp)
                    break;

                count--;
                firstIndex = firstIndex == capacity - 1 ? 0 : firstIndex + 1;
            }

            if (mode == MODE.MINIMUM) {
                while (count > 0) {
                    int newLast;

                    if ((newLast = lastIndex - 1) == -1)
                        newLast = capacity - 1;

                    if (queueBuffer[newLast] > value) {
                        lastIndex = newLast;
                        count--;
                    } else
                        break;
                }
            } else {
                while (count > 0) {
                    int newLast;

                    if ((newLast = lastIndex - 1) == -1)
                        newLast = capacity - 1;

                    if (queueBuffer[newLast] < value) {
                        lastIndex = newLast;
                        count--;
                    } else
                        break;
                }
            }

            if (count > 0 && firstIndex == lastIndex) {
                requestCompleted = 0;

                int newCapacity = capacity << 1;

                dateTimeBuffer = Arrays.copyOf(dateTimeBuffer, newCapacity);
                queueBuffer = Arrays.copyOf(queueBuffer, newCapacity);
                pointsBuffer = Arrays.copyOf(pointsBuffer, newCapacity);

                System.arraycopy(queueBuffer, 0, queueBuffer, capacity, lastIndex);
                System.arraycopy(pointsBuffer, 0, pointsBuffer, capacity, lastIndex);
                System.arraycopy(dateTimeBuffer, 0, dateTimeBuffer, capacity, lastIndex);

                /*for (int Index = 0; Index < lastIndex; Index++)
                   {
                    queueBuffer[capacity + Index] = queueBuffer[Index];
                    pointsBuffer[capacity + Index] = pointsBuffer[Index];
                    dateTimeBuffer[capacity + Index] = dateTimeBuffer[Index];
                   }*/

                lastIndex = capacity + lastIndex;
                firstIndex %= capacity;
                capacity = newCapacity;
                lastIndex %= capacity;
            }

            // store new element and increase count
            queueBuffer[lastIndex] = value;
            pointsBuffer[lastIndex] = currentPoint++;
            dateTimeBuffer[lastIndex] = timestamp;
            count++;

            lastIndex = lastIndex == capacity - 1 ? 0 : lastIndex + 1;

            return true;
        } else {
            if (put(value, currentPoint++)) {
                dateTimeBuffer[lastIndex == 0 ? capacity - 1 : lastIndex - 1] = timestamp;

                return true;
            } else
                return false;
        }
    }

    public boolean put(double value) {
        if (isTimeManaged)
            return false;

        return put(value, currentPoint++);
    }

    public double extremum() {
        return count == 0 ? Double.NaN : queueBuffer[firstIndex];
    }

    public int extremumPoint() {
        return count == 0 ? -1 : currentPoint - pointsBuffer[firstIndex] - 1;
    }

    public long extremumDateTime() {
        return count == 0 ? 0 : dateTimeBuffer[firstIndex];
    }

    public enum MODE {
        MINIMUM, MAXIMUM
    }
}

