/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.snmp.model.timebase;

public class Failure_SnmpSupport implements com.epam.deltix.snmp.s4jrt.EntrySupport <Failure> {
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