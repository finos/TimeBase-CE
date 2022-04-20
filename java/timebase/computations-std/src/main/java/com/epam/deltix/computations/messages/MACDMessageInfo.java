package com.epam.deltix.computations.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import java.lang.Override;

/**
 */
public interface MACDMessageInfo extends MessageInterface {
  /**
   * @return Histogram
   */
  double getHistogram();

  /**
   * @return true if Histogram is not null
   */
  boolean hasHistogram();

  /**
   * @return Value
   */
  double getValue();

  /**
   * @return true if Value is not null
   */
  boolean hasValue();

  /**
   * @return Signal
   */
  double getSignal();

  /**
   * @return true if Signal is not null
   */
  boolean hasSignal();

  /**
   * Method copies state to a given instance
   */
  @Override
  MACDMessageInfo clone();
}
