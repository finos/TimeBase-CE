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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.timebase.messages.SchemaElement;

/**
 */
@SchemaElement(
    title = "Playback Event Type"
)
public enum PlaybackEventType {
  /**
   * Playback Started
   */
  @SchemaElement(
      name = "STARTED"
  )
  STARTED(0),

  /**
   * Playback Stopped
   */
  @SchemaElement(
      name = "STOPPED"
  )
  STOPPED(1),

  /**
   * Playback Paused
   */
  @SchemaElement(
      name = "PAUSED"
  )
  PAUSED(2),

  /**
   * Playback Resumed
   */
  @SchemaElement(
      name = "RESUMED"
  )
  RESUMED(3),

  /**
   * Playback Speed was changed
   */
  @SchemaElement(
      name = "SPEED_CHANGED"
  )
  SPEED_CHANGED(4),

  /**
   * Playback Frequency was changed
   */
  @SchemaElement(
      name = "FREQUENCY_CHANGED"
  )
  FREQUENCY_CHANGED(5);

  private final int value;

  PlaybackEventType(int value) {
    this.value = value;
  }

  public int getNumber() {
    return this.value;
  }

  public static PlaybackEventType valueOf(int number) {
    switch (number) {
      case 0: return STARTED;
      case 1: return STOPPED;
      case 2: return PAUSED;
      case 3: return RESUMED;
      case 4: return SPEED_CHANGED;
      case 5: return FREQUENCY_CHANGED;
      default: return null;
    }
  }

  public static PlaybackEventType strictValueOf(int number) {
    final PlaybackEventType value = valueOf(number);
    if (value == null) {
      throw new IllegalArgumentException("Enumeration 'PlaybackEventType' does not have value corresponding to '" + number + "'.");
    }
    return value;
  }
}