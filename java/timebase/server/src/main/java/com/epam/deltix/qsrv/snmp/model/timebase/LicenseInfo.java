package com.epam.deltix.qsrv.snmp.model.timebase;

import com.epam.deltix.snmp.pub.Description;
import com.epam.deltix.snmp.pub.Id;

/**
 *
 */
public interface LicenseInfo {

    @Id(1)
    @Description("Date until license is valid")
    public String   getValidUtil();

    @Id(2)
    @Description("Licensee information")
    public String   getLicensee();

    @Id(3)
    @Description("Last time if license validation")
    public String   getLastValidated();

    @Id(4)
    @Description("Number of days license ")
    public int      getDaysValid();

    @Id(5)
    @Description("License state")
    public String   getLicenseState();

    @Id(6)
    @Description("License type")
    public String   getType();

    @Id(7)
    @Description("License Feature")
    public String   getFeatures();
}
