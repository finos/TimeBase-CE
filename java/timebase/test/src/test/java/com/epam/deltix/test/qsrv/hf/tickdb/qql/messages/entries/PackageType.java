package com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries;

import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(name = "deltix.entries.PackageType")
public enum PackageType {
    VENDOR_SNAPSHOT,
    INCREMENTAL_UPDATE
}
