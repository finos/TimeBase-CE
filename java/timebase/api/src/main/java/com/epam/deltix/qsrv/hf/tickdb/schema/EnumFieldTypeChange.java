package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;

import java.util.Objects;

public class EnumFieldTypeChange extends FieldTypeChange {

    protected EnumFieldTypeChange() { } // for jaxb

    public EnumFieldTypeChange(DataField source, DataField target) {
        super(source, target);
    }

    @Override
    public Impact getChangeImpact() {
        return super.getChangeImpact();
    }

    public Impact getChangeImpact(SchemaMapping mapping) {
        EnumDataType src = (EnumDataType) getSource().getType();
        EnumDataType trg = (EnumDataType) getTarget().getType();

        if (src.descriptor.equals(trg.descriptor))
            return Impact.None;

        EnumValue[] srcValues = src.descriptor.getValues();
        EnumValue[] trgValues = trg.descriptor.getValues();

        boolean[] mappedValues = new boolean[srcValues.length];

        label:
        for (int i = 0; i < srcValues.length; i++) {
            EnumValue value = mapping.enumValues.get(srcValues[i]);
            if (value != null) {
                for (EnumValue trgValue : trgValues) {
                    if (Objects.equals(trgValue.symbol, value.symbol)) {
                        mappedValues[i] = true;
                        continue label;
                    }
                }
            }
            for (EnumValue trgValue : trgValues) {
                if (Objects.equals(trgValue.symbol, srcValues[i].symbol)) {
                    mappedValues[i] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < srcValues.length; i++) {
            if (!mappedValues[i] && !trg.isNullable())
                return Impact.DataLoss;
        }

        return Impact.DataConvert;
    }
}
