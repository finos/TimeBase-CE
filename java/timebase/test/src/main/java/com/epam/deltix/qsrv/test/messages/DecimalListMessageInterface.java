package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.util.collections.generated.LongArrayList;

/**
 */
public interface DecimalListMessageInterface extends DecimalListMessageInfo, MessageInterface {
  /**
   * @param value - Decimal List
   */
  void setDecimalList(LongArrayList value);

  /**
   */
  void nullifyDecimalList();

  /**
   * @param value - Decimal Nullable List
   */
  void setDecimalNullableList(LongArrayList value);

  /**
   */
  void nullifyDecimalNullableList();

  /**
   * Method nullifies all instance properties
   */
  @Override
  DecimalListMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  DecimalListMessageInterface reset();

  @Override
  DecimalListMessageInterface copyFrom(RecordInfo template);
}
