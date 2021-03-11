package com.epam.deltix.qsrv.snmp.model.timebase;

public class Failure_SnmpSupport implements deltix.snmp.s4jrt.EntrySupport <Failure> {
 public static final Failure_SnmpSupport INSTANCE = new Failure_SnmpSupport ();

 private Failure_SnmpSupport () {
 }

 @Override public org.snmp4j.smi.OID getIndex (Failure entry) {
  return (new org.snmp4j.smi.OID ( new int [] { entry.getIndex () }));
 }

 @Override public org.snmp4j.smi.Variable getValue (Failure entry, int column) {
  switch (column) {
   case 0:
    return (new org.snmp4j.smi.Integer32 (entry.getIndex ()));
   case 1:
    return (new org.snmp4j.smi.OctetString (entry.getMessage ()));
   default: throw new RuntimeException ("Illegal column: " + column);
  }
 }

 @Override public int getNumColumns () {
  return (2);
 }
}
