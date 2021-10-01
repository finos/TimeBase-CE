package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.util.collections.generated.LongList;

/**
 */
public interface DecimalListMessageInfo extends MessageInfo {
  /**
   * @return Decimal List
   */
  LongList getDecimalList();

  /**
   * @return true if Decimal List is not null
   */
  boolean hasDecimalList();

  /**
   * @return Decimal Nullable List
   */
  LongList getDecimalNullableList();

  /**
   * @return true if Decimal Nullable List is not null
   */
  boolean hasDecimalNullableList();

  /**
   * Method copies state to a given instance
   */
  @Override
  DecimalListMessageInfo clone();
}
