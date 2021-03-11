package com.epam.deltix.qsrv.hf.topic.consumer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks that method may be called only by the the thread that executes reading
 * or any other thread that received this data source from the reading thread.
 *
 * In general, this annotation means that method may not be executed when other thread concurrently reads data.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Inherited
public @interface ReaderThreadOnly {
}
