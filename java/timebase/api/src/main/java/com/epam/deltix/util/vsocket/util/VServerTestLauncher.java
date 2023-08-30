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
package com.epam.deltix.util.vsocket.util;

import com.epam.deltix.util.vsocket.VSServer;

import java.io.IOException;


public class VServerTestLauncher {
    private VSServer startedServer;
    private int port = 8025;
    private int packetSize = 4096;

    public static void main(String[] args) {
        if(args.length == 0 || (args.length == 1 && args[0] == "h"))
        {
            printHelp();
        }

        StartServer(args);
    }

    private static void StartServer(String[] args)
    {
        VServerTestLauncher vserverInstance = new VServerTestLauncher();
        if (args.length > 1)
            vserverInstance.packetSize = Integer.parseInt(args[1]);

        if (args.length > 2)
            vserverInstance.port = Integer.parseInt(args[2]);

        System.out.println("Going to start server at port " + vserverInstance.port);
        try{
            switch (args[0])
            {
                case "lv":
                    vserverInstance.startLatencyServer();
                    break;

                case "ls":
                    vserverInstance.startLatencySocket();
                    break;

                case "ev":
                    vserverInstance.startEchoServer();
                    break;

                case "es":
                    vserverInstance.startEchoSocket();
                    break;

                case "iv":
                    vserverInstance.startInputThroughputServer();
                    break;

                case "is":
                    vserverInstance.startInputThroughputSocket();
                    break;

                case "ov":
                    vserverInstance.startOutputThroughputServer();
                    break;

                case "os":
                    vserverInstance.startOutputThroughputSocket();
                    break;

                case "nv" :
                    vserverInstance.startEmptyServer();
                    break;

                case "rv" :
                    vserverInstance.startReadingServer();
                    break;

                default:
                    System.out.println("Unknown argument: " + args[0]);
            }
        }
        catch (IOException e) {
            System.out.println("Cannot start VServer.");
            System.out.print(e.toString());
        }
    }

    private static void printHelp()
    {
        System.out.println("Help guide:");
        System.out.println("Arguments: [e]");
        System.out.println("h  - Help.");
        System.out.println("nv - Runs VSocket server, that only listens to clients.");
        System.out.println("lv - Runs VSocket server, configured for latency test.");
        System.out.println("ls - Runs Socket server,  configured for latency test.");
        System.out.println("ev - Runs VSocket server, configured for echo test.");
        System.out.println("es - Runs Socket server,  configured for echo test.");
        System.out.println("iv - Runs VSocket server, configured for input throughput test.");
        System.out.println("is - Runs Socket server,  configured for input throughput test.");
        System.out.println("ov - Runs VSocket server, configured for output throughput test.");
        System.out.println("os - Runs Socket server,  configured for output throughput test.");
        System.out.println("rv - Runs VSocket Reading server, configured for reconnecting tests.");
    }


    private void startLatencyServer() throws IOException {
        Thread server = TestVServerSocketFactory.createLatencyVServer(port, packetSize);
        server.setDaemon(false);
        server.start();
    }

    private void startLatencySocket() throws IOException {
        Thread server = TestServerSocketFactory.createLatencyServerSocket(port, packetSize);
        server.setDaemon(false);
        server.start();
    }

    private void startEchoServer() throws IOException {
        Thread server = TestVServerSocketFactory.createEchoVServer(port);
        server.setDaemon(false);
        server.start();
    }

    private void startEchoSocket() throws IOException {
        Thread server = TestServerSocketFactory.createEchoServerSocket(port);
        server.setDaemon(false);
        server.start();
    }

    private void startInputThroughputServer() throws IOException {
        Thread server = TestVServerSocketFactory.createInputThroughputVServer(port, packetSize);
        server.setDaemon(false);
        server.start();
    }

    private void startInputThroughputSocket() throws IOException {
//        Thread server = TestServerSocketFactory.createThroughputServerSocket(port, packetSize);
//        server.setDaemon(false);
//        server.start();
    }

    private void startOutputThroughputServer() throws IOException {
        Thread server = TestVServerSocketFactory.createOutputThroughputVServer(port, packetSize);
        server.setDaemon(false);
        server.start();
    }

    private void startOutputThroughputSocket() throws IOException {
        Thread server = TestServerSocketFactory.createThroughputServerSocket(port, packetSize);
        server.setDaemon(false);
        server.start();
    }

    private void startReadingServer() throws IOException {
        Thread server = TestVServerSocketFactory.createReadingVServer(port, packetSize);
        server.setDaemon(false);
        server.start();
    }

    private void startEmptyServer() throws IOException {
        Thread server = TestVServerSocketFactory.createEmptyVServer(port);
        server.setDaemon(false);
        server.start();
    }



}