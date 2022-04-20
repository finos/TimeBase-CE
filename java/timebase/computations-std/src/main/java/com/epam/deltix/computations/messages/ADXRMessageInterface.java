package com.epam.deltix.computations.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import com.epam.deltix.timebase.messages.RecordInfo;
import java.lang.Override;

/**
 */
public interface ADXRMessageInterface extends ADXRMessageInfo, MessageInterface {
  /**
   * @param value - Adxr
   */
  void setAdxr(double value);

  /**
   */
  void nullifyAdxr();

  /**
   * @param value - Adx
   */
  void setAdx(double value);

  /**
   */
  void nullifyAdx();

  /**
   * @param value - Dx
   */
  void setDx(double value);

  /**
   */
  void nullifyDx();

  /**
   * @param value - Plus DI
   */
  void setPlusDI(double value);

  /**
   */
  void nullifyPlusDI();

  /**
   * @param value - Minus DI
   */
  void setMinusDI(double value);

  /**
   */
  void nullifyMinusDI();

  /**
   * Method nullifies all instance properties
   */
  @Override
  ADXRMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  ADXRMessageInterface reset();

  @Override
  ADXRMessageInterface copyFrom(RecordInfo template);
}
