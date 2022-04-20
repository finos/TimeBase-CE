package com.epam.deltix.computations.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import com.epam.deltix.timebase.messages.RecordInfo;

import java.lang.Override;

/**
 */
public interface BollingerMessageInterface extends BollingerMessageInfo, MessageInterface {
  /**
   * @param value - Upper Band
   */
  void setUpperBand(double value);

  /**
   */
  void nullifyUpperBand();

  /**
   * @param value - Lower Band
   */
  void setLowerBand(double value);

  /**
   */
  void nullifyLowerBand();

  /**
   * @param value - Middle Band
   */
  void setMiddleBand(double value);

  /**
   */
  void nullifyMiddleBand();

  /**
   * @param value - Band Width
   */
  void setBandWidth(double value);

  /**
   */
  void nullifyBandWidth();

  /**
   * @param value - Percent B
   */
  void setPercentB(double value);

  /**
   */
  void nullifyPercentB();

  /**
   * Method nullifies all instance properties
   */
  @Override
  BollingerMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  BollingerMessageInterface reset();

  @Override
  BollingerMessageInterface copyFrom(RecordInfo template);
}
