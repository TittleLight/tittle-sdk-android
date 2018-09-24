package com.clarityhk.tittlesdksample;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class TittleCommands {
    public static byte[] createStartStandardConfigPacket(final String ssid, final String password) throws UnknownHostException {
        String handsetIp = Util.getIPAddress();
        return createStartStandardConfigPacket(ssid, password, InetAddress.getByName(handsetIp));
    }

    public static byte[] createStartStandardConfigPacket(final String ssid, final String password, final InetAddress handsetIp) {
        byte[] ssidBytes = ssid.getBytes();
        byte[] passwordBytes = password.getBytes();
        byte[] ipBytes = handsetIp.getAddress();

        // Get handset IP in the Tittle-AP network

        byte[] data = new byte[ssidBytes.length + passwordBytes.length + 10];
        // Standard config packet header
        data[0] = (byte) 0x70;
        data[1] = (byte) 0x07;

        int currentBytes = 2;
        System.arraycopy(ssidBytes, 0, data, currentBytes, ssidBytes.length);

        currentBytes += ssidBytes.length;
        System.arraycopy(passwordBytes, 0, data, ++currentBytes, passwordBytes.length);

        currentBytes += passwordBytes.length + 1;

        data[currentBytes++] = ipBytes[0];
        data[currentBytes++] = ipBytes[1];
        data[currentBytes++] = ipBytes[2];
        data[currentBytes] = ipBytes[3];

        // Standard config packet tail
        data[data.length - 2] = (byte) 0x0D;
        data[data.length - 1] = (byte) 0x0A;
        return data;
    }

    public static byte[] createStopStandardConfigPacket() {
        byte[] data = new byte[4];
        data[0] = 0x70;
        data[1] = 0x09;
        data[2] = 0x0d;
        data[3] = 0x0a;
        return data;
    }



    public static DeviceInfo parseDeviceInfo(byte[] deviceInfoBytes) {
        return parseDeviceInfo(deviceInfoBytes, new byte[] { 0x70, 0x08 });
    }

    public static DeviceInfo parseDeviceInfo(byte[] deviceInfoBytes, byte[] expectedHeader) {
        int endOfNameIdx = 0;

        for (int i = 0; i < deviceInfoBytes.length; i++) {
            if (deviceInfoBytes[i] == 0x00) {
                endOfNameIdx = i;
                break;
            }
        }

        // Check the header is what we expect
        byte[] headerBytes = Arrays.copyOfRange(deviceInfoBytes, 0, 2);
        if (headerBytes[0] != expectedHeader[0] || headerBytes[1] != expectedHeader[1])
            throw new MalformedResponseException("Unexpected response. Response does not have device info header, response: " + Util.toHexString(deviceInfoBytes));

        // Check packet length. Packet should have 2 byte header, 2 byte tail, 10 bytes for
        // addresses and variable length name.
        if (deviceInfoBytes.length - endOfNameIdx != 4 + 6 + 3)
            throw new MalformedResponseException("Malformed response. Response length is not correct, response: " + Util.toHexString(deviceInfoBytes));

        byte[] deviceNameBytes = Arrays.copyOfRange(deviceInfoBytes, 2, endOfNameIdx);
        // Skip the 0x00 terminating the name string
        byte[] macAddressBytes = Arrays.copyOfRange(deviceInfoBytes, endOfNameIdx + 1, endOfNameIdx + 7);
        byte[] ipAddressBytes = Arrays.copyOfRange(deviceInfoBytes, endOfNameIdx + 7, endOfNameIdx + 7 + 4);

        String name = new String(deviceNameBytes);
        String macAddress = getAddress(macAddressBytes);
        String ipAddress = getAddress(ipAddressBytes);

        return new DeviceInfo(name, macAddress, ipAddress);
    }

    public static byte[] createLightOnPacket(int red, int green, int blue, int intensity) {
        byte[] data = new byte[7];
        data[0] = (byte) 0x10;
        data[1] = (byte) red;
        data[2] = (byte) green;
        data[3] = (byte) blue;
        data[4] = (byte) intensity;
        data[5] = (byte) 0x0D;
        data[6] = (byte) 0x0A;

        return data;
    }

    private static String getAddress(byte[] b) {
        StringBuilder address = new StringBuilder();

        for (byte aB : b) {
            address.append(String.valueOf(aB & 0xFF)).append(".");
        }
        return address.substring(0, address.length() - 1);
    }

    public static byte[] createScanBroadcastPacket(InetAddress handsetIp) {
        byte[] ipAddress = handsetIp.getAddress();
        byte[] packet = new byte[8];
        packet[0] = 0x70;
        packet[1] = 0x04;
        packet[2] = ipAddress[0];
        packet[3] = ipAddress[1];
        packet[4] = ipAddress[2];
        packet[5] = ipAddress[3];
        packet[6] = 0x0D;
        packet[7] = 0x0A;
        return packet;
    }
}

class MalformedResponseException extends RuntimeException {
    public MalformedResponseException(String s) {
        super(s);
    }
}

