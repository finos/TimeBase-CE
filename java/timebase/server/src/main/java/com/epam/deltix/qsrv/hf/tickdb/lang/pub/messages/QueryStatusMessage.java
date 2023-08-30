/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub.messages;

import com.epam.deltix.containers.BinaryAsciiString;
import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.containers.MutableString;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.timebase.messages.SchemaElement;

@SchemaElement(
    title = "Query Status Message"
)
public class QueryStatusMessage extends InstrumentMessage {
    public static final String CLASS_NAME = QueryStatusMessage.class.getName();

    /**
     * Details about the event in text form.
     */
    protected CharSequence cause = "";

    /**
     * Query Status
     */
    protected QueryStatus status = null;

    /**
     * Details about the event in text form.
     * @return Cause
     */
    @SchemaElement(
        name = "cause",
        title = "Cause"
    )
    public CharSequence getCause() {
        return cause;
    }

    /**
     * Details about the event in text form.
     * @param value - Cause
     */
    public void setCause(CharSequence value) {
        this.cause = value;
    }

    /**
     * Details about the event in text form.
     * @return true if Cause is not null
     */
    public boolean hasCause() {
        return cause != null;
    }

    /**
     * Details about the event in text form.
     */
    public void nullifyCause() {
        this.cause = null;
    }

    /**
     * Query Status
     * @return Status
     */
    @SchemaElement(
        name = "status",
        title = "Status"
    )
    public QueryStatus getStatus() {
        return status;
    }

    /**
     * Query Status
     * @param value - Status
     */
    public void setStatus(QueryStatus value) {
        this.status = value;
    }

    /**
     * Query Status
     * @return true if Status is not null
     */
    public boolean hasStatus() {
        return status != null;
    }

    /**
     * Query Status
     */
    public void nullifyStatus() {
        this.status = null;
    }

    /**
     * Creates new instance of this class.
     * @return new instance of this class.
     */
    @Override
    protected QueryStatusMessage createInstance() {
        return new QueryStatusMessage();
    }

    /**
     * Method nullifies all instance properties
     */
    @Override
    public QueryStatusMessage nullify() {
        super.nullify();
        nullifyCause();
        nullifyStatus();
        return this;
    }

    /**
     * Resets all instance properties to their default values
     */
    @Override
    public QueryStatusMessage reset() {
        super.reset();
        cause = "";
        status = null;
        return this;
    }

    /**
     * Method copies state to a given instance
     */
    @Override
    public QueryStatusMessage clone() {
        QueryStatusMessage t = createInstance();
        t.copyFrom(this);
        return t;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        boolean superEquals = super.equals(obj);
        if (!superEquals) return false;
        if (!(obj instanceof QueryStatusMessage)) return false;
        QueryStatusMessage other =(QueryStatusMessage)obj;
        if (hasCause() != other.hasCause()) return false;
        if (hasCause()) {
            if (getCause().length() != other.getCause().length()) return false; else {
                CharSequence s1 = getCause();
                CharSequence s2 = other.getCause();
                if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
                    if (!s1.equals(s2)) return false;
                } else {
                    if (!CharSequenceUtils.equals(s1, s2)) return false;
                }
            }
        }
        if (hasStatus() != other.hasStatus()) return false;
        if (hasStatus() && getStatus() != other.getStatus()) return false;
        return true;
    }

    /**
     * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        if (hasCause()) {
            hash = hash * 31 + getCause().hashCode();
        }
        if (hasStatus()) {
            hash = hash * 31 + getStatus().ordinal();
        }
        return hash;
    }

    /**
     * Method copies state to a given instance
     * @param template class instance that should be used as a copy source
     */
    @Override
    public QueryStatusMessage copyFrom(RecordInfo template) {
        super.copyFrom(template);
        if (template instanceof QueryStatusMessage) {
            QueryStatusMessage t = (QueryStatusMessage)template;
            if (t.hasCause()) {
                if (hasCause() && getCause() instanceof StringBuilder) {
                    ((StringBuilder)getCause()).setLength(0);
                } else {
                    setCause(new StringBuilder());
                }
                ((StringBuilder)getCause()).append(t.getCause());
            } else {
                nullifyCause();
            }
            if (t.hasStatus()) {
                setStatus(t.getStatus());
            } else {
                nullifyStatus();
            }
        }
        return this;
    }

    /**
     * @return a string representation of this class object.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        return toString(str).toString();
    }

    /**
     * @return a string representation of this class object.
     */
    @Override
    public StringBuilder toString(StringBuilder str) {
        str.append("{ \"$type\":  \"QueryStatusMessage\"");
        if (hasCause()) {
            str.append(", \"cause\": \"").append(getCause()).append("\"");
        }
        if (hasStatus()) {
            str.append(", \"status\": \"").append(getStatus()).append("\"");
        }
        if (hasTimeStampMs()) {
            str.append(", \"timestamp\": \"").append(formatNanos(getTimeStampMs(), (int)getNanoTime())).append("\"");
        }
//        if (hasInstrumentType()) {
//            str.append(", \"instrumentType\": \"").append(getInstrumentType()).append("\"");
//        }
        if (hasSymbol()) {
            str.append(", \"symbol\": \"").append(getSymbol()).append("\"");
        }
        str.append("}");
        return str;
    }
}