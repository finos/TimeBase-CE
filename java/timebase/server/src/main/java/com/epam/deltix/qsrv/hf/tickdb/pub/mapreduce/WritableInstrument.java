package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;


import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentKey;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class WritableInstrument extends InstrumentKey implements WritableComparable<IdentityKey> {

    public WritableInstrument() {
    }

    public WritableInstrument(CharSequence symbol) {
        super(symbol);
    }

    public WritableInstrument(IdentityKey copy) {
        super(copy);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(symbol.toString());
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.symbol = in.readUTF();
    }
}


