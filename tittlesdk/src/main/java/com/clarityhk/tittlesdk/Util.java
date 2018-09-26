package com.clarityhk.tittlesdk;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class Util {
    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02X ", b));
        return sb.toString();
    }

    public static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addresses = Collections.list(intf.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress()) {
                        String sAddr = address.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (isIPv4) return sAddr;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static InetAddress getBroadcastAddress(InetAddress handsetIp) {

        try {
            List<InterfaceAddress> addresses = NetworkInterface.getByInetAddress(handsetIp).getInterfaceAddresses();

            for (InterfaceAddress inetAddress : addresses) {
                if (inetAddress.getBroadcast() != null) {
                    return inetAddress.getBroadcast();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
