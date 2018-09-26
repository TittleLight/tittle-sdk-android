package com.clarityhk.tittlesdk;

import android.util.Log;

public class TittleLightControl implements RunCommand.CommandListener {
    private final static String TAG = TittleLightControl.class.getSimpleName();

    private final static int PORT = 9999;
    private final Connection connection;
    private final TittleLightControlListener listener;

    public TittleLightControl(String tittleIp, TittleLightControlListener listener) {
        this.connection = new Connection(tittleIp, PORT);
        this.listener = listener;
    }

    public void disconnect() {
        this.connection.disconnect();
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
        new Thread(new RunCommand(this.connection, TittleCommands.createLightOnPacket(red, green, blue, intensity), this)).start();
    }

    @Override
    public void onResponseReceived(byte[] data) {
        this.listener.lightModeSet();
    }

    public void commandFailed() {
        Log.d(TAG, "Command failed");
        this.listener.failedToSetLightMode();
    }

    public interface TittleLightControlListener {
        void lightModeSet();
        void failedToSetLightMode();
    }
}


