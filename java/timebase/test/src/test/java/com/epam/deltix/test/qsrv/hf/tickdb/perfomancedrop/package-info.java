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

@javax.xml.bind.annotation.XmlSchema (
        namespace = "http://xml.deltixlab.com/internal/quantserver/3.0",
        elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED
)
@javax.xml.bind.annotation.XmlAccessorType (javax.xml.bind.annotation.XmlAccessType.NONE)
package com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop;

/**
 * This package contains a group of tests that were created to track down
 * a problem with Performance Drop(PD)after live message.
 * <p>PD problem description: if we have a group of readers for historic data from stream X and
 * create transient live reader for stream Y then performance(throughput) of readers of stream X
 * may noticeably(by 10-25%) drop despite the streams are supposed to be independent.</p>
 * <p>The actual performance drop heavily depends on machine.
 *
 * <p>Steps to reproduce PD:
 * <ol>
 *   <li> Start at least two threads that read historic data from stream X("tick" stream in tests).
 *   <li> Measure throughput of X stream readers.
 *   <li> After some delay(~10 sec or more)start sending and reading messages to/from a transient live stream Y.
 * </ol>
 * Usually needed at least 1 and at most 10 live messages.
 * There should be pause between messages
 * <p>Known information:
 * <ul>
 * <li> Reproducible on Windows machines. Not tested on Linux.
 * <li> Problem is stochastic. It may just not reproduce in one run but reproduce in same conditions on next run.
 * <li> There needed at least 2 readers of stream X to reproduce PD.
 * <li> Sometimes the problem may appear after the very first live message. Sometimes it needs 4-5 messages.
 * <li> There must be a reader for live messages. If there is no reader then PD not occurs.
 * <li> Producer and Consumer of live messages must be in different threads.
 * <li> GC activity does not causes the problem(removal of GC activity not helps).
 * <li> Biased locking is not the cause(-XX:-UseBiasedLocking and -XX:BiasedLockingStartupDelay=0 does not affect the result of test)
 * </ul>

 * <p>Useful JVM options:
 * <pre>
 * #Boost inlining - eliminates the PD in some cases
 * -XX:FreqInlineSize=3000 -XX:InlineSmallCode=5000

 * # Print compilation log data for JitWatch - use this if you want to get data for <a href="https://github.com/AdoptOpenJDK/jitwatch">JitWatch</a>
 * -XX:+UnlockDiagnosticVMOptions -XX:+TraceClassLoading -XX:+LogCompilation -XX:LogFile=tb_compilation.log -XX:+PrintAssembly
 * # Note: PrintAssembly requires special hsdis*.dll fro JDK. See http://stackoverflow.com/a/24524285/443428
 * </pre>
 *
 * @author Alexei Osipov
 */


