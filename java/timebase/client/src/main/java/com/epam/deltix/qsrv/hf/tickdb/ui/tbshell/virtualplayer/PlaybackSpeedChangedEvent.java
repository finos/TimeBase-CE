/*
 * Copyright 2021 EPAM Systems, Inc
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
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.timebase.messages.RecordInterface;
import com.epam.deltix.timebase.messages.TypeConstants;

/**
 */
@SchemaElement(
    name = "deltix.timebase.api.messages.playback.PlaybackSpeedChangedEvent",
    title = "Event"
)
public class PlaybackSpeedChangedEvent extends PlaybackEvent implements RecordInterface {
  public static final String CLASS_NAME = PlaybackSpeedChangedEvent.class.getName();

  /**
   */
  protected double speed = TypeConstants.IEEE64_NULL;

  public PlaybackSpeedChangedEvent() {
    super();
    eventType = PlaybackEventType.SPEED_CHANGED;
  }

  /**
   * @return Event Type
   */
  @Override
  public PlaybackEventType getEventType() {
    return eventType;
  }

  /**
   * @param value - Event Type
   */
  @Override
  public void setEventType(PlaybackEventType value) {
    this.eventType = value;
  }

  /**
   * @return true if Event Type is not null
   */
  @Override
  public boolean hasEventType() {
    return eventType != null;
  }

  /**
   */
  @Override
  public void nullifyEventType() {
    this.eventType = null;
  }

  /**
   * @return Speed
   */
  @SchemaElement
  public double getSpeed() {
    return speed;
  }

  /**
   * @param value - Speed
   */
  public void setSpeed(double value) {
    this.speed = value;
  }

  /**
   * @return true if Speed is not null
   */
  public boolean hasSpeed() {
    return !Double.isNaN(speed);
  }

  /**
   */
  public void nullifySpeed() {
    this.speed = TypeConstants.IEEE64_NULL;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected PlaybackSpeedChangedEvent createInstance() {
    return new PlaybackSpeedChangedEvent();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public PlaybackSpeedChangedEvent nullify() {
    super.nullify();
    nullifySpeed();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public PlaybackSpeedChangedEvent reset() {
    super.reset();
    speed = TypeConstants.IEEE64_NULL;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public PlaybackSpeedChangedEvent clone() {
    PlaybackSpeedChangedEvent t = createInstance();
    t.copyFrom(this);
    return t;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    boolean superEquals = super.equals(obj);
    if (!superEquals) return false;
    if (!(obj instanceof PlaybackSpeedChangedEvent)) return false;
    PlaybackSpeedChangedEvent other =(PlaybackSpeedChangedEvent)obj;
    if (hasSpeed() != other.hasSpeed()) return false;
    if (hasSpeed() && getSpeed() != other.getSpeed()) return false;
    return true;
  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    if (hasSpeed()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getSpeed()) ^ (Double.doubleToLongBits(getSpeed()) >>> 32)));
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public PlaybackSpeedChangedEvent copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof PlaybackSpeedChangedEvent) {
      PlaybackSpeedChangedEvent t = (PlaybackSpeedChangedEvent)template;
      if (t.hasSpeed()) {
        setSpeed(t.getSpeed());
      } else {
        nullifySpeed();
      }
    }
    return this;
  }

  /**
   * @return a string representation of this class object.
   */
  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    return toString(str).toString();
  }

  /**
   * @return a string representation of this class object.
   */
  @Override
  public StringBuilder toString(StringBuilder str) {
    str.append("{ \"$type\":  \"PlaybackSpeedChangedEvent\"");
    if (hasSpeed()) {
      str.append(", \"speed\": ").append(getSpeed());
    }
    if (hasEventType()) {
      str.append(", \"eventType\": \"").append(getEventType()).append("\"");
    }
    if (hasPlaybackTime()) {
      str.append(", \"playbackTime\": \"").append(getPlaybackTime()).append("\"");
    }
    if (hasTimeStampMs()) {
      str.append(", \"timestamp\": \"").append(formatNanos(getTimeStampMs(), (int)getNanoTime())).append("\"");
    }
    if (hasSymbol()) {
      str.append(", \"symbol\": \"").append(getSymbol()).append("\"");
    }
    str.append("}");
    return str;
  }
}