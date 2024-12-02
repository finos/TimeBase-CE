/*
 * Copyright 2024 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.ConstraintViolationException;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.StreamChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IncompatibleValueException;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.ValueUndefinedException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ConversionConfirmation;

import java.util.*;

/**
 *
 */
public class StreamChangeProcessor {
    public static StreamChangeTask process (
            DXTickStream                            stream,
            StreamOptions                           optionsBean,
            Map <DataField, ModifyFieldData>        defaults,
            ConversionConfirmation                  confirm
    )
    {
        MetaDataChange.ContentType inType = stream.isFixedType() ? MetaDataChange.ContentType.Fixed : MetaDataChange.ContentType.Polymorphic;
        MetaDataChange.ContentType outType = optionsBean.isFixedType() ? MetaDataChange.ContentType.Fixed : MetaDataChange.ContentType.Polymorphic;
        StreamOptions options = stream.getStreamOptions();
        StreamMetaDataChange change = SchemaAnalyzer.DEFAULT.getChanges(options.getMetaData(), inType, optionsBean.getMetaData(), outType);
        validateChanges(change, defaults, confirm);
        validateStreamOptions(optionsBean, options, confirm);

        return createStreamChangeTask(optionsBean, change);
    }

    public static StreamChangeTask process (
            DXTickStream                            stream,
            StreamOptions                           optionsBean,
            Map <DataField, ModifyFieldData>        defaults,
            SchemaMapping                           mapping,
            ConversionConfirmation                  confirm
    )
    {
        MetaDataChange.ContentType inType = stream.isFixedType() ? MetaDataChange.ContentType.Fixed : MetaDataChange.ContentType.Polymorphic;
        MetaDataChange.ContentType outType = optionsBean.isFixedType() ? MetaDataChange.ContentType.Fixed : MetaDataChange.ContentType.Polymorphic;
        StreamOptions options = stream.getStreamOptions();
        StreamMetaDataChange change = new SchemaAnalyzer(mapping, true).getChanges(options.getMetaData(), inType, optionsBean.getMetaData(), outType);
        validateChanges(change, defaults, confirm);
        validateStreamOptions(optionsBean, options, confirm);

        return createStreamChangeTask(optionsBean, change);
    }

    private static void validateChanges(StreamMetaDataChange change,
                                        Map<DataField, ModifyFieldData> defaults,
                                        ConversionConfirmation confirm) {

        for (ClassDescriptorChange cd : change.changes) {
            if (cd.getTarget() == null && !canDropType(confirm)) {
                throw new ConstraintViolationException("Cannot drop type: " + cd.getSource() + " due to conversion constraint: " + confirm, 0);
            }

            for (AbstractFieldChange c : cd.getChanges()) {
                if (c instanceof DeleteFieldChange) {
                    if (!canConvert(confirm)) {
                        throw new ConstraintViolationException("Cannot convert field: " + c.getSource().getName() + " due to conversion constraint: " + confirm, 0);
                    }
                    if (!canDropField(confirm)) {
                        throw new ConstraintViolationException("Cannot drop field: " + c.getSource().getName() + " due to conversion constraint: " + confirm, 0);
                    }
                } else if (c instanceof FieldTypeChange) {
                    if (c.getChangeImpact() != SchemaChange.Impact.None && !canConvert(confirm)) {
                        throw new ConstraintViolationException("Cannot convert field: " + c.getSource().getName() + " due to conversion constraint: " + confirm, 0);
                    }
                } else if (c instanceof FieldModifierChange) {
                    if (!canConvert(confirm)) {
                        throw new ConstraintViolationException("Cannot convert field: " + c.getSource().getName() + " due to conversion constraint: " + confirm, 0);
                    }
                    if (!canDropField(confirm)) {
                        throw new ConstraintViolationException("Cannot drop field: " + c.getSource().getName() + " due to conversion constraint: " + confirm, 0);
                    }
                } else if (c instanceof CreateFieldChange) {
                    if (c.getChangeImpact() != SchemaChange.Impact.None && !canConvert(confirm)) {
                        throw new ConstraintViolationException("Cannot add field: " + c.getTarget().getName() + " due to conversion constraint: " + confirm, 0);
                    }
                } else if (c.getClass() == FieldChange.class) {
                    FieldAttribute attribute = c.getAttribute();
                    if (attribute != FieldAttribute.Title && attribute != FieldAttribute.Description) {
                        if (!canConvert(confirm)) {
                            throw new ConstraintViolationException("Cannot convert field: " + c.getSource().getName() + " due to conversion constraint: " + confirm, 0);
                        }
                    }
                } else {
                    if (!canConvert(confirm)) {
                        throw new ConstraintViolationException("Cannot convert field: " + c.getSource().getName() + " due to conversion constraint: " + confirm, 0);
                    }
                }

                if (c.hasErrors()) {
                    DataField target = c.getTarget();
                    String fullName = cd.getTarget().getName() + " [" + target.getName() + "]";
                    ModifyFieldData fieldData = defaults.get(target);

                    CompiledConstant constant = fieldData != null ? fieldData.defValue : null;
                    if (constant == null && !target.getType().isNullable()) {
                        if (c.hasErrors()) {
                            throw new ValueUndefinedException(fullName + ": default value is undefined", 0);
                        }
                    }

                    if (c.hasErrors()) {
                        if (c instanceof FieldTypeChange) {
                            ((FieldTypeChange) c).setDefaultValue(constant == null || constant.isNull() ? null : constant.getString());
                            if (c.hasErrors()) {
                                throw new IncompatibleValueException(fullName + ": default value expected.", 0);
                            }
                        } else if (c instanceof CreateFieldChange) {
                            ((CreateFieldChange) c).setInitialValue(constant == null || constant.isNull() ? null : constant.getString());
                            if (c.hasErrors()) {
                                throw new ValueUndefinedException(fullName + ": default value expected.", 0);
                            }
                        } else if (c instanceof FieldModifierChange) {
                            if (target instanceof StaticDataField) {
                                ((FieldModifierChange) c).setInitialValue(((StaticDataField) target).getStaticValue());
                            } else {
                                ((FieldModifierChange) c).setInitialValue(constant == null || constant.isNull() ? null : constant.getString());
                            }

                            if (c.hasErrors()) {
                                throw new ValueUndefinedException(fullName + ": default value expected.", 0);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void validateStreamOptions(StreamOptions optionsBean, StreamOptions options, ConversionConfirmation confirm) {
        if (options.scope == StreamScope.DURABLE && options.distributionFactor != optionsBean.distributionFactor) {
            if (confirm != ConversionConfirmation.DROP_DATA)
                throw new ConstraintViolationException("Cannot change Distribution Factor: " + options.distributionFactor + " due to conversion constraint: " + confirm, 0);
        }
    }

    private static StreamChangeTask createStreamChangeTask(StreamOptions optionsBean, StreamMetaDataChange change) {
        StreamChangeTask task = new StreamChangeTask();
        task.name = optionsBean.name;
        task.description = optionsBean.description;
        task.ha = optionsBean.highAvailability;
        task.periodicity = optionsBean.periodicity == null ? null : optionsBean.periodicity;
        task.bufferOptions = optionsBean.bufferOptions;
        task.change = change;

        task.df = optionsBean.distributionFactor;
        return task;
    }

    private static boolean canConvert(ConversionConfirmation confirm) {
        return confirm == ConversionConfirmation.CONVERT_DATA ||
                confirm == ConversionConfirmation.DROP_ATTRIBUTES ||
                confirm == ConversionConfirmation.DROP_TYPES ||
                confirm == ConversionConfirmation.DROP_DATA;
    }

    private static boolean canDropField(ConversionConfirmation confirm) {
        return confirm == ConversionConfirmation.DROP_ATTRIBUTES || confirm == ConversionConfirmation.DROP_TYPES || confirm == ConversionConfirmation.DROP_DATA;
    }

    private static boolean canDropType(ConversionConfirmation confirm) {
        return confirm == ConversionConfirmation.DROP_TYPES || confirm == ConversionConfirmation.DROP_DATA;
    }
}
