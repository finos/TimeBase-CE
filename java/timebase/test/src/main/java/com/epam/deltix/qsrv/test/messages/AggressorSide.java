package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.timebase.messages.OldElementName;
import com.epam.deltix.timebase.messages.SchemaElement;

/**
 * Side of quote or trade. Buy or Sell.
 */
@OldElementName("deltix.qsrv.hf.pub.AggressorSide")
@SchemaElement(
        name = "deltix.timebase.api.messages.AggressorSide",
        title = "Aggressor Side"
)
public enum AggressorSide {
    /**
     * Buy side.
     */
    @SchemaElement(
            name = "BUY"
    )
    BUY(0),

    /**
     * Sell side.
     */
    @SchemaElement(
            name = "SELL"
    )
    SELL(1);

    private final int value;

    AggressorSide(int value) {
        this.value = value;
    }

    public int getNumber() {
        return this.value;
    }

    public static AggressorSide valueOf(int number) {
        switch (number) {
            case 0: return BUY;
            case 1: return SELL;
            default: return null;
        }
    }

    public static AggressorSide strictValueOf(int number) {
        final AggressorSide value = valueOf(number);
        if (value == null) {
            throw new IllegalArgumentException("Enumeration 'AggressorSide' does not have value corresponding to '" + number + "'.");
        }
        return value;
    }
}
