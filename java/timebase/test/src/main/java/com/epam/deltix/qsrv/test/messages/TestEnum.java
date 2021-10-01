package com.epam.deltix.qsrv.test.messages;

/**
 */
public enum TestEnum {
  /**
   */
  ZERO(0),

  /**
   */
  ONE(1),

  /**
   */
  TWO(2),

  /**
   */
  THREE(3),

  /**
   */
  FOUR(4),

  /**
   */
  FIVE(5);

  private final int value;

  TestEnum(int value) {
    this.value = value;
  }

  public int getNumber() {
    return this.value;
  }

  public static TestEnum valueOf(int number) {
    switch (number) {
      case 0: return ZERO;
      case 1: return ONE;
      case 2: return TWO;
      case 3: return THREE;
      case 4: return FOUR;
      case 5: return FIVE;
      default: return null;
    }
  }

  public static TestEnum strictValueOf(int number) {
    final TestEnum value = valueOf(number);
    if (value == null) {
      throw new IllegalArgumentException("Enumeration 'TestEnum' does not have value corresponding to '" + number + "'.");
    }
    return value;
  }
}
