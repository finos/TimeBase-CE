package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@SchemaElement(name = "deltix.entries.Package")
public class Package extends InstrumentMessage {

    private PackageType packageType;
    private ObjectArrayList<PackageEntry> entries;

    @SchemaElement(title = "packageType")
    public PackageType getPackageType() {
        return packageType;
    }

    public void setPackageType(PackageType packageType) {
        this.packageType = packageType;
    }

    @SchemaElement(title = "entries")
    @SchemaArrayType(
        isNullable = false,
        isElementNullable = false,
        elementTypes =  {
            TradeEntry.class, L1Entry.class, L2Entry.class
        }
    )
    public ObjectArrayList<PackageEntry> getEntries() {
        return entries;
    }

    public void setEntries(ObjectArrayList<PackageEntry> entries) {
        this.entries = entries;
    }

    @Override
    public Package copyFrom(RecordInfo source) {
        super.copyFrom(source);
        if (source instanceof Package) {
            final Package obj = (Package) source;
            packageType = obj.packageType;
            entries.addAll(obj.entries);
        }
        return this;
    }
}
