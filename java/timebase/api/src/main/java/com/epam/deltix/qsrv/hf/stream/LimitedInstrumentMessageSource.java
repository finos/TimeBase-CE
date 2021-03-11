package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.InstrumentMessageSourceAdapter;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableCursor;
import com.epam.deltix.util.concurrent.NextResult;

/**
 * MessageSource that simulates end-of-stream at given timestamp
 */
public class LimitedInstrumentMessageSource extends InstrumentMessageSourceAdapter implements IntermittentlyAvailableCursor {
    private final long limit;
    private boolean keepReading = true;
    private final IntermittentlyAvailableCursor iac;

    public LimitedInstrumentMessageSource(InstrumentMessageSource delegate, long endTime) {
        super(delegate);

        this.limit = endTime;
        this.iac = (delegate instanceof IntermittentlyAvailableCursor) ? (IntermittentlyAvailableCursor)delegate : null;
    }

    @Override
    public InstrumentMessage getMessage() {
        assert keepReading;
        return super.getMessage();
    }

    @Override
    public boolean isAtEnd() {
        return ! keepReading;
    }

    @Override
    public boolean next() {
        if (keepReading) {
            keepReading = super.next();
            if (keepReading) {
                InstrumentMessage msg = super.getMessage();
                keepReading = (msg.getTimeStampMs() < limit);
            }
        }
        return keepReading;
    }

    @Override
    public NextResult nextIfAvailable() {

        if (keepReading) {
            NextResult result = iac.nextIfAvailable();

            keepReading = result == NextResult.OK;
            if (keepReading) {
                InstrumentMessage msg = super.getMessage();
                keepReading = (msg.getTimeStampMs() < limit);
            }

            return result;
        }

        return NextResult.END_OF_CURSOR;
    }
}
