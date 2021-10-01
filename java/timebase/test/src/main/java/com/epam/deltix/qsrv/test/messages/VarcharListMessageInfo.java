package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.util.collections.generated.LongList;
import com.epam.deltix.util.collections.generated.ObjectList;

/**
 */
public interface VarcharListMessageInfo extends MessageInfo {
  /**
   * @return Alphanumeric List
   */
  LongList getAlphanumericList();

  /**
   * @return true if Alphanumeric List is not null
   */
  boolean hasAlphanumericList();

  /**
   * @return Alphanumeric Nullable List
   */
  LongList getAlphanumericNullableList();

  /**
   * @return true if Alphanumeric Nullable List is not null
   */
  boolean hasAlphanumericNullableList();

  /**
   * @return Char Sequence List
   */
  ObjectList<CharSequence> getCharSequenceList();

  /**
   * @return true if Char Sequence List is not null
   */
  boolean hasCharSequenceList();

  /**
   * @return Char Sequence Nullable List
   */
  ObjectList<CharSequence> getCharSequenceNullableList();

  /**
   * @return true if Char Sequence Nullable List is not null
   */
  boolean hasCharSequenceNullableList();

  /**
   * Method copies state to a given instance
   */
  @Override
  VarcharListMessageInfo clone();
}
