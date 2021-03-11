package com.epam.deltix.util.net;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;

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
