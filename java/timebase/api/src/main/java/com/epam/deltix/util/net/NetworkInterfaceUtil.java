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
package com.epam.deltix.util.net;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import javax.annotation.Nullable;
import java.net.*;
import java.util.Enumeration;

/**
 * @author Alexei Osipov
 */
public class NetworkInterfaceUtil {
    private static final Log LOG = LogFactory.getLog(NetworkInterfaceUtil.class);

    /**
     * Tries to determine any public IPv4 address for current host.
     *
     * @return any public IP address of current host or null
     */
    @Nullable
    public static String getOwnPublicAddressAsText() {
        InetAddress ownIpAddress = NetworkInterfaceUtil.getOwnPublicAddress();
        if (ownIpAddress == null) {
            return null;
        }

        //noinspection UnnecessaryLocalVariable
        String hostAddress = ownIpAddress.getHostAddress();
        /*
        // Remove IPv6 scope
        if (hostAddress.indexOf('%') != -1) {
            hostAddress = hostAddress.substring(0, hostAddress.indexOf('%'));
        }
        */
        return hostAddress;
    }

    /**
     * Tries to determine any public IPv4 address for current host.
     *
     * @return any public IP address of current host or null
     */
    @Nullable
    private static InetAddress getOwnPublicAddress() {
        final Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isVirtual() && !ni.isLoopback() && !ni.isPointToPoint()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress() && !address.isMulticastAddress() && address instanceof Inet4Address) {
                            return address;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            return null;
        }
        return null;
    }

    /**
     * Check if address represents local IP or hostname that resolves to local.
     *
     */
    public static boolean isLocal(@Nullable String ipAddress) {
        try {
            return InetAddress.getByName(ipAddress).isLoopbackAddress();
        } catch (UnknownHostException x) {
            LOG.warn("Failed to determine if address '%s' is local.").with(ipAddress);
            return false;
        }
    }
}