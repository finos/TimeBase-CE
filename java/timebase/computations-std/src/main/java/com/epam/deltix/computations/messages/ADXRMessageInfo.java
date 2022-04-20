package com.epam.deltix.computations.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import java.lang.Override;

/**
 */
public interface ADXRMessageInfo extends MessageInterface {
  /**
   * @return Adxr
   */
  double getAdxr();

  /**
   * @return true if Adxr is not null
   */
  boolean hasAdxr();

  /**
   * @return Adx
   */
  double getAdx();

  /**
   * @return true if Adx is not null
   */
  boolean hasAdx();

  /**
   * @return Dx
   */
  double getDx();

  /**
   * @return true if Dx is not null
   */
  boolean hasDx();

  /**
   * @return Plus DI
   */
  double getPlusDI();

  /**
   * @return true if Plus DI is not null
   */
  boolean hasPlusDI();

  /**
   * @return Minus DI
   */
  double getMinusDI();

  /**
   * @return true if Minus DI is not null
   */
  boolean hasMinusDI();

  /**
   * Method copies state to a given instance
   */
  @Override
  ADXRMessageInfo clone();
}
