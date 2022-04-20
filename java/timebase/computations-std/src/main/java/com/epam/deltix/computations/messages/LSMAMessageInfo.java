package com.epam.deltix.computations.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import java.lang.Override;

/**
 */
public interface LSMAMessageInfo extends MessageInterface {
  /**
   * @return Slope
   */
  double getSlope();

  /**
   * @return true if Slope is not null
   */
  boolean hasSlope();

  /**
   * @return RSquared
   */
  double getRSquared();

  /**
   * @return true if RSquared is not null
   */
  boolean hasRSquared();

  /**
   * @return Value
   */
  double getValue();

  /**
   * @return true if Value is not null
   */
  boolean hasValue();

  /**
   * Method copies state to a given instance
   */
  @Override
  LSMAMessageInfo clone();
}
