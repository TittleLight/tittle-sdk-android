package com.clarityhk.tittlesdksample;

public class DeviceInfo {
    public final String name;
    public final String macAddress;
    public final String ipAddress;

    public DeviceInfo(String name, String macAddress, String ipAddress) {
        this.name = name;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        return String.format("Device info:\n\tDevice name: %s\n\tMac address: %s\n\tIp address: %s", name, macAddress, ipAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!DeviceInfo.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        DeviceInfo other = (DeviceInfo) obj;

        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.macAddress == null) ? (other.macAddress != null) : !this.macAddress.equals(other.macAddress)) {
            return false;
        }
        if ((this.ipAddress == null) ? (other.ipAddress != null) : !this.ipAddress.equals(other.ipAddress)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 53 * hash + (this.macAddress != null ? this.macAddress.hashCode() : 0);
        hash = 53 * hash + (this.ipAddress != null ? this.ipAddress.hashCode() : 0);
        return hash;
    }
}
