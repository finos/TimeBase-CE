package com.epam.deltix.test.qsrv.hf.pub;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.CompiledCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.util.json.JSONRawMessageParser;
import com.epam.deltix.qsrv.util.json.JSONRawMessagePrinter;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by Alex Karpovich on 10/8/2018.
 */
@Category(JUnitCategories.TickDBFast.class)
public class Test_JSON {

    @Test
    public void verify() throws Exception {
        RecordClassDescriptor rcd = (RecordClassDescriptor) Introspector.introspectSingleClass(Test_RecordCodecsArrays.Execution.class);

        Test_RecordCodecsArrays.Execution msg = new Test_RecordCodecsArrays.Execution();
        msg.setSymbol("ZZZ");
        msg.setTimeStampMs(System.currentTimeMillis());

        msg.extraTags.add(new Test_RecordCodecsArrays.ExtraTag(1, "1"));
        msg.extraTags.add(null);
        msg.extraTags.add(new Test_RecordCodecsArrays.ExtraTag(2, "2"));

        Test_RecordCodecsArrays.Group g = new Test_RecordCodecsArrays.Group();
        g.extraTags.add(new Test_RecordCodecsArrays.ExtraTag(11, "g11"));
        g.extraTags.add(new Test_RecordCodecsArrays.ExtraTag(12, "g12"));
        g.extraTags.add(null);
        msg.partyIDs.add(g);

        g = new Test_RecordCodecsArrays.Group();
        g.extraTags.add(new Test_RecordCodecsArrays.ExtraTag(21, "g21"));
        g.extraTags.add(null);
        g.extraTags.add(new Test_RecordCodecsArrays.ExtraTag(22, "g22"));
        msg.partyIDs.add(g);

        msg.ids.add(1);
        msg.ids.add(5);
        msg.ids.add(536546);

        encodeDecode(rcd, msg);

        msg = new Test_RecordCodecsArrays.Execution();
        msg.partyIDs = null;
        msg.extraTags = null;

        encodeDecode(rcd, msg);
    }

    private void encodeDecode(RecordClassDescriptor rcd, InstrumentMessage msg) {
        MemoryDataOutput out = new MemoryDataOutput();
        FixedBoundEncoder encoder = CompiledCodecMetaFactory.INSTANCE.createFixedBoundEncoderFactory(TypeLoaderImpl.DEFAULT_INSTANCE, rcd).create();
        encoder.encode(msg, out);
        RawMessage source = new RawMessage(rcd);
        source.setBytes(out);

        JSONRawMessagePrinter printer = new JSONRawMessagePrinter();
        StringBuilder sb = new StringBuilder();
        printer.append(source, sb);

        System.out.println(sb);

        JsonParser parser = new JsonParser();
        JsonArray messages = (JsonArray) parser.parse("[" + sb.toString() + "]");

        JSONRawMessageParser p = new JSONRawMessageParser(new RecordClassDescriptor[] {rcd});

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            RawMessage raw = p.parse((JsonObject) messages.get(i));
            printer.append(raw, result);
        }

        Assert.assertEquals(sb.toString(), result.toString());
    }
}
