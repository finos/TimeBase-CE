package com.epam.deltix.computations.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import com.epam.deltix.timebase.messages.RecordInfo;

import java.lang.Override;

/**
 */
public interface LSMAMessageInterface extends LSMAMessageInfo, MessageInterface {
  /**
   * @param value - Slope
   */
  void setSlope(double value);

  /**
   */
  void nullifySlope();

  /**
   * @param value - RSquared
   */
  void setRSquared(double value);

  /**
   */
  void nullifyRSquared();

  /**
   * @param value - Value
   */
  void setValue(double value);

  /**
   */
  void nullifyValue();

  /**
   * Method nullifies all instance properties
   */
  @Override
  LSMAMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  LSMAMessageInterface reset();

  @Override
  LSMAMessageInterface copyFrom(RecordInfo template);
}
