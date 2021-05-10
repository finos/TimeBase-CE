package com.epam.deltix.qsrv.snmp.model.timebase;

public class Loader_SnmpSupport implements com.epam.deltix.snmp.s4jrt.EntrySupport <Loader> {
 public static final Loader_SnmpSupport INSTANCE = new Loader_SnmpSupport ();

 private Loader_SnmpSupport () {
 }

 @Override public org.snmp4j.smi.OID getIndex (Loader entry) {
  return (new org.snmp4j.smi.OID ( new int [] { entry.getLoaderId () }));
 }

 @Override public org.snmp4j.smi.Variable getValue (Loader entry, int column) {
  switch (column) {
   case 0:
    return (new org.snmp4j.smi.Integer32 (entry.getLoaderId ()));
   case 1:
    return (new org.snmp4j.smi.OctetString (entry.getSource ()));
   case 2:
    return (new org.snmp4j.smi.OctetString (entry.getLoaderLastMessageTime ()));
   default: throw new RuntimeException ("Illegal column: " + column);
  }
 }

 @Override public int getNumColumns () {
  return (3);
 }
}
