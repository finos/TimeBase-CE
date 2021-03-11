package com.epam.deltix.snmp;

/**
 * Container object.
 * Different services may store SNMP-data objects into to make them available for SNMP agent.
 *
 * @author Alexei Osipov
 */
public class QuantServerSnmpObjectContainer {
    // TODO: Consider using Map<String, Object> instead
    private Object timeBaseSnmpInfo;
    private Object aggregatorSnmpInfo;

    public Object getTimeBaseSnmpInfo() {
        return timeBaseSnmpInfo;
    }

    public void setTimeBaseSnmpInfo(Object timeBaseSnmpInfo) {
        this.timeBaseSnmpInfo = timeBaseSnmpInfo;
    }

    public Object getAggregatorSnmpInfo() {
        return aggregatorSnmpInfo;
    }

    public void setAggregatorSnmpInfo(Object aggregatorSnmpInfo) {
        this.aggregatorSnmpInfo = aggregatorSnmpInfo;
    }

}
