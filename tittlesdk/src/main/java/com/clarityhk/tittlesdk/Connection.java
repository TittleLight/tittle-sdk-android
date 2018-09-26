package com.clarityhk.tittlesdk;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;

class Connection {
    private final String TAG = Connection.class.getSimpleName();
    private final String tittleIp;
    private final int port;

    Socket socket;

    Connection(String tittleIp, int port) {
        this.tittleIp = tittleIp;
        this.port = port;
    }

    public void connect() throws IOException {
        if (socket == null || socket.isClosed()) {
            Log.d(TAG, "Socket closed, recreating");
            socket = new Socket(tittleIp, port);
            socket.setReuseAddress(true);
        }
        Log.d(TAG, "Connected to " + this.tittleIp + ":" + this.port);
    }
}
