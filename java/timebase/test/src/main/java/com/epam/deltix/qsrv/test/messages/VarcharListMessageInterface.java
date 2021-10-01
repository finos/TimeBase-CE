package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

/**
 */
public interface VarcharListMessageInterface extends VarcharListMessageInfo, MessageInterface {
  /**
   * @param value - Alphanumeric List
   */
  void setAlphanumericList(LongArrayList value);

  /**
   */
  void nullifyAlphanumericList();

  /**
   * @param value - Alphanumeric Nullable List
   */
  void setAlphanumericNullableList(LongArrayList value);

  /**
   */
  void nullifyAlphanumericNullableList();

  /**
   * @param value - Char Sequence List
   */
  void setCharSequenceList(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyCharSequenceList();

  /**
   * @param value - Char Sequence Nullable List
   */
  void setCharSequenceNullableList(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyCharSequenceNullableList();

  /**
   * Method nullifies all instance properties
   */
  @Override
  VarcharListMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  VarcharListMessageInterface reset();

  @Override
  VarcharListMessageInterface copyFrom(RecordInfo template);
}
