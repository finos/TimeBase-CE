package com.epam.deltix.qsrv.hf.tickdb.schema;

/**
 * Created by IntelliJ IDEA.
 * User: KarpovichA   
 */
public interface SchemaChange {

    public Impact getChangeImpact();

    public enum Impact {
        None,
        DataConvert,
        /**
         * Indicates, that we cannot apply this change to schema.
         */
        DataLoss,
    }
}