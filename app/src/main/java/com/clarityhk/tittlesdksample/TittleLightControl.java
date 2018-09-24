package com.clarityhk.tittlesdksample;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

interface CommandResponseListener {
    void onResponseReceived(byte[] data);
}

class TittleLightControl implements CommandResponseListener {
    private final static String TAG = TittleLightControl.class.getSimpleName();

    private final static int PORT = 9999;
    private final Connection connection;

    public TittleLightControl(String tittleIp) {
        this.connection = new Connection(tittleIp, PORT);
    }

    private boolean isValidValue(int value) {
        return value >= 0 && value <= 255;
    }

    public void setLightMode(int red, int green, int blue, int intensity) {
        if (!isValidValue(red))
            throw new RuntimeException("Invalid color red, expected value between 0 and 255");
        if (!isValidValue(green))
            throw new RuntimeException("Invalid color green, expected value between 0 and 255");
        if (!isValidValue(blue))
            throw new RuntimeException("Invalid color blue, expected value between 0 and 255");
        if (!isValidValue(intensity))
            throw new RuntimeException("Invalid intensity, expected value between 0 and 255");

        Log.d(TAG, "Setting light mode");
        new Thread(new RunCommand(this.connection, TittleCommands.createLightOnPacket(red, green, blue, intensity))).start();
    }

    @Override
    public void onResponseReceived(byte[] data) {
        Log.d(TAG, "Light mode set");
    }
}


