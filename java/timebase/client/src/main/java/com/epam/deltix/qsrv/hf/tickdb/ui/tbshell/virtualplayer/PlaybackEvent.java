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

import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.timebase.messages.RecordInterface;

/**
 */
@SchemaElement(
    name = "deltix.timebase.api.messages.playback.PlaybackEvent",
    title = "Playback Event"
)
public class PlaybackEvent extends InstrumentMessage implements RecordInterface {
  public static final String CLASS_NAME = PlaybackEvent.class.getName();

  /**
   */
  protected PlaybackEventType eventType = null;

  /**
   */
  protected long playbackTime = TypeConstants.TIMESTAMP_UNKNOWN;

  /**
   * @return Playback Event Type
   */
  @SchemaElement(
      title = "Playback Event Type"
  )
  public PlaybackEventType getEventType() {
    return eventType;
  }

  /**
   * @param value - Playback Event Type
   */
  public void setEventType(PlaybackEventType value) {
    this.eventType = value;
  }

  /**
   * @return true if Playback Event Type is not null
   */
  public boolean hasEventType() {
    return eventType != null;
  }

  /**
   */
  public void nullifyEventType() {
    this.eventType = null;
  }

  /**
   * @return Playback Time
   */
  @SchemaElement(
      title = "Playback Time"
  )
  @SchemaType(
      dataType = SchemaDataType.TIMESTAMP
  )
  public long getPlaybackTime() {
    return playbackTime;
  }

  /**
   * @param value - Playback Time
   */
  public void setPlaybackTime(long value) {
    this.playbackTime = value;
  }

  /**
   * @return true if Playback Time is not null
   */
  public boolean hasPlaybackTime() {
    return playbackTime != TypeConstants.TIMESTAMP_UNKNOWN;
  }

  /**
   */
  public void nullifyPlaybackTime() {
    this.playbackTime = TypeConstants.TIMESTAMP_UNKNOWN;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected PlaybackEvent createInstance() {
    return new PlaybackEvent();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public PlaybackEvent nullify() {
    super.nullify();
    nullifyEventType();
    nullifyPlaybackTime();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public PlaybackEvent reset() {
    super.reset();
    eventType = null;
    playbackTime = TypeConstants.TIMESTAMP_UNKNOWN;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public PlaybackEvent clone() {
    PlaybackEvent t = createInstance();
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
    if (!(obj instanceof PlaybackEvent)) return false;
    PlaybackEvent other =(PlaybackEvent)obj;
    if (hasEventType() != other.hasEventType()) return false;
    if (hasEventType() && getEventType() != other.getEventType()) return false;
    if (hasPlaybackTime() != other.hasPlaybackTime()) return false;
    if (hasPlaybackTime() && getPlaybackTime() != other.getPlaybackTime()) return false;
    return true;
  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    if (hasEventType()) {
      hash = hash * 31 + getEventType().getNumber();
    }
    if (hasPlaybackTime()) {
      hash = hash * 31 + ((int)(getPlaybackTime() ^ (getPlaybackTime() >>> 32)));
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public PlaybackEvent copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof PlaybackEvent) {
      PlaybackEvent t = (PlaybackEvent)template;
      if (t.hasEventType()) {
        setEventType(t.getEventType());
      } else {
        nullifyEventType();
      }
      if (t.hasPlaybackTime()) {
        setPlaybackTime(t.getPlaybackTime());
      } else {
        nullifyPlaybackTime();
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
    str.append("{ \"$type\":  \"PlaybackEvent\"");
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