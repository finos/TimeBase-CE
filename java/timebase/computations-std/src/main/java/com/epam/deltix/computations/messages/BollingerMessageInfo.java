package com.epam.deltix.computations.messages;

import com.epam.deltix.timebase.messages.MessageInterface;

import java.lang.Override;

/**
 */
public interface BollingerMessageInfo extends MessageInterface {
  /**
   * @return Upper Band
   */
  double getUpperBand();

  /**
   * @return true if Upper Band is not null
   */
  boolean hasUpperBand();

  /**
   * @return Lower Band
   */
  double getLowerBand();

  /**
   * @return true if Lower Band is not null
   */
  boolean hasLowerBand();

  /**
   * @return Middle Band
   */
  double getMiddleBand();

  /**
   * @return true if Middle Band is not null
   */
  boolean hasMiddleBand();

  /**
   * @return Band Width
   */
  double getBandWidth();

  /**
   * @return true if Band Width is not null
   */
  boolean hasBandWidth();

  /**
   * @return Percent B
   */
  double getPercentB();

  /**
   * @return true if Percent B is not null
   */
  boolean hasPercentB();

  /**
   * Method copies state to a given instance
   */
  @Override
  BollingerMessageInfo clone();
}
