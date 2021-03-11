package com.epam.deltix.qsrv.hf;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.topic.consumer.MappingProvider;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

/**
 * @author Alexei Osipov
 */
public class StubData {


//    public static RecordClassDescriptor makeTradeMessageDescriptor ()
//    {
//        RecordClassDescriptor marketMsgDescriptor = mkMarketMessageDescriptor (840);
//
//
//        return (StreamConfigurationHelper.mkTradeMessageDescriptor (marketMsgDescriptor, "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO));
//    }
//
//    private static RecordClassDescriptor     mkMarketMessageDescriptor (
//            Integer                 staticCurrencyCode
//    )
//    {
//        final String            name = MarketMessage.class.getName ();
//        final DataField[]      fields = {
//                new NonStaticDataField(
//                        "originalTimestamp", "Original Time",
//                        new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
//                        StreamConfigurationHelper.mkField (
//                        "currencyCode", "Currency Code",
//                        new IntegerDataType (IntegerDataType.ENCODING_INT16, true), null,
//                        staticCurrencyCode
//                )
//        };
//
//        return (new RecordClassDescriptor (name, name, true, null, fields));
//    }

    public static MappingProvider getStubMappingProvider() {
        return new MappingProvider() {
            @Override
            public ConstantIdentityKey[] getMappingSnapshot() {
                return new ConstantIdentityKey[0];
            }

            @Override
            public IntegerToObjectHashMap<ConstantIdentityKey> getTempMappingSnapshot(int neededTempEntityIndex) {
                return new IntegerToObjectHashMap<>();
            }
        };
    }
}
