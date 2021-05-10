package com.epam.deltix.qsrv.snmp.model.timebase;

public class Cursor_SnmpSupport implements com.epam.deltix.snmp.s4jrt.EntrySupport <Cursor> {
 public static final Cursor_SnmpSupport INSTANCE = new Cursor_SnmpSupport ();

 private Cursor_SnmpSupport () {
 }

 @Override public org.snmp4j.smi.OID getIndex (Cursor entry) {
  return (new org.snmp4j.smi.OID ( new int [] { entry.getCursorId () }));
 }

 @Override public org.snmp4j.smi.Variable getValue (Cursor entry, int column) {
  switch (column) {
   case 0:
    return (new org.snmp4j.smi.Integer32 (entry.getCursorId ()));
   case 1:
    return (new org.snmp4j.smi.OctetString (entry.getCursorLastMessageTime ()));
   default: throw new RuntimeException ("Illegal column: " + column);
  }
 }

 @Override public int getNumColumns () {
  return (2);
 }
}
