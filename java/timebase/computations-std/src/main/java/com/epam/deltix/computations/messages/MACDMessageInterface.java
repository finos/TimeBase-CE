package com.epam.deltix.computations.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import com.epam.deltix.timebase.messages.RecordInfo;
import java.lang.Override;

/**
 */
public interface MACDMessageInterface extends MACDMessageInfo, MessageInterface {
  /**
   * @param value - Histogram
   */
  void setHistogram(double value);

  /**
   */
  void nullifyHistogram();

  /**
   * @param value - Value
   */
  void setValue(double value);

  /**
   */
  void nullifyValue();

  /**
   * @param value - Signal
   */
  void setSignal(double value);

  /**
   */
  void nullifySignal();

  /**
   * Method nullifies all instance properties
   */
  @Override
  MACDMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  MACDMessageInterface reset();

  @Override
  MACDMessageInterface copyFrom(RecordInfo template);
}
