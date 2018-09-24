package com.clarityhk.tittlesdksample;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class StandardConfig implements SetupListener {
    private static final String TAG = StandardConfig.class.getSimpleName();
    private static final String TITTLE_AP_IP = "192.168.1.1";
    private static final int TITTLE_AP_PORT = 9999;
    private static final int CLIENT_PORT = 9999;

    private final String password;
    private final String ssid;
    private Socket socket;
    private ServerSocket serverSocket;
    private StandardConfigListener listener;
    private DeviceInfo deviceInfo;

    private Thread receiveSSIDAckThread;
    private Thread sendSSIDThread;
    private Thread receiveTittleInfoThread;

    private AtomicBoolean canceled = new AtomicBoolean(false);
    private final int timeout;

    public StandardConfig(String ssid, String password, int timeout, StandardConfigListener listener) {
        this.timeout = timeout;
        this.ssid = ssid;
        this.password = password;
        this.listener = listener;
    }

    public void connect() {
        this.socket = new Socket();
        new Thread(new ConnectRunnable(this)).start();
    }

    public void cancel() {
        cancel(false);
    }

    public void cancel(boolean callListener) {
        if (callListener) canceled.set(true);
        Log.d(TAG, "Cancelled.");
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (receiveTittleInfoThread != null) receiveTittleInfoThread.interrupt();
        if (receiveSSIDAckThread != null) receiveSSIDAckThread.interrupt();
        if (sendSSIDThread != null) sendSSIDThread.interrupt();
    }

    public void onConnected() {
        Log.d(TAG, "Socket connected...");
        if (this.canceled.get()) return;

        // Listen to for a response to SSID and password message on the socket
        receiveSSIDAckThread = new Thread(new ReceiveSSIDRunnableResponse(this));
        receiveSSIDAckThread.start();

        // Send SSID and password to Tittle
        sendSSIDThread = new Thread(new SendSSIDRunnable(this, this.ssid, this.password));
        sendSSIDThread.start();

        // Start listening for a response to Tittle IP
        receiveTittleInfoThread = new Thread(new ReceiveTittleInfo(this));
        receiveTittleInfoThread.start();
    }

    public void onSSIDSent() {
        Log.d(TAG, "SSID and password sent...");
    }

    public void onSSIDReceived() {
        Log.d(TAG, "SSID and password received by Tittle...");
    }

    public void onDeviceInfoReceived(DeviceInfo deviceInfo) {
        Log.d(TAG, "Tittle connected to wifi...");
        Log.d(TAG, deviceInfo.toString());
        this.deviceInfo = deviceInfo;
        try {
            new Thread(new FinishConfigRunnable(new Socket(TITTLE_AP_IP, TITTLE_AP_PORT), this)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onFinishSent() {
        Log.d(TAG, "Config finished");
        listener.onConfigComplete(this.deviceInfo.ipAddress);
    }

    class FinishConfigRunnable implements Runnable {
        private final SetupListener listener;
        private final Socket socket;

        FinishConfigRunnable(Socket socket, SetupListener listener) {
            this.socket = socket;
            this.listener = listener;
        }

        @Override
        public void run() {
            Log.d(TAG, "Sending finish config");
            try {
                sendSSID();
            } catch (IOException e) {
                Log.e(TAG, "Error while sending SSID to Tittle");
                e.printStackTrace();
            }
        }

        private void sendSSID() throws IOException {
            byte[] buffer = TittleCommands.createStopStandardConfigPacket();
            OutputStream out = socket.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.write(buffer, 0, buffer.length);
            this.listener.onFinishSent();
        }
    }

    class ReceiveTittleInfo implements Runnable {
        private final SetupListener listener;

        ReceiveTittleInfo(SetupListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            Log.d(TAG, "Listening on broadcast socket");
            receiveTittleInfo();
            Log.d(TAG, "Stopped listening on broadcast socket");
        }

        private void receiveTittleInfo() {
            while (!canceled.get() && !Thread.currentThread().isInterrupted() && (serverSocket == null || (serverSocket.isBound() && !serverSocket.isClosed()))) {
                try {
                    serverSocket = new ServerSocket();
                    serverSocket.setSoTimeout(timeout);
                    serverSocket.bind(new InetSocketAddress(TITTLE_AP_PORT));
                    Socket socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    int bytesRead;
                    byte buffer[] = new byte[1024];
                    bytesRead = in.read(buffer);
                    if (bytesRead != -1) {
                        serverSocket.close();
                        byte[] result = Arrays.copyOf(buffer, bytesRead);
                        try {
                            DeviceInfo deviceInfo = TittleCommands.parseDeviceInfo(result);
                            if (listener != null) listener.onDeviceInfoReceived(deviceInfo);
                        } catch (MalformedResponseException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Error while listening for Tittle IP");
                    e.printStackTrace();
                }
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class ReceiveSSIDRunnableResponse implements Runnable {
        private final SetupListener listener;

        ReceiveSSIDRunnableResponse(SetupListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                receiveAck();
            } catch (IOException e) {
                Log.d(TAG, "Error while listening to SSID response");
                e.printStackTrace();
                cancel();
            }
        }

        private void receiveAck() throws IOException {
            Log.d(TAG, "Listening on socket");
            InputStream input = socket.getInputStream();
            while (!canceled.get() && !Thread.currentThread().isInterrupted()) {
                byte[] data = new byte[4];
                input.read(data, 0, data.length);
                Log.d(TAG, "Message: " + Util.toHexString(data));
                if (data[0] == 0x00 && data[1] == 0x00 && data[2] == 0x0d && data[3] == 0x0a) {
                    if (listener != null) listener.onSSIDReceived();
                    break;
                }
            }
        }
    }

    class SendSSIDRunnable implements Runnable {
        private final SetupListener listener;
        private final String ssid;
        private final String password;
        private InetAddress handsetIp;

        SendSSIDRunnable(SetupListener listener, String ssid, String password) {
            this(listener, ssid, password, null);
        }

        SendSSIDRunnable(SetupListener listener, String ssid, String password, InetAddress handsetIp) {
            this.listener = listener;
            this.ssid = ssid;
            this.password = password;
            this.handsetIp = handsetIp;
        }

        @Override
        public void run() {
            try {
                sendSSID(this.ssid, this.password);
                this.listener.onSSIDSent();
            } catch (IOException e) {
                Log.e(TAG, "Error while sending SSID to Tittle");
                e.printStackTrace();
                cancel(true);
            }
        }

        private void sendSSID(String ssid, String password) throws IOException {
            byte[] buffer = TittleCommands.createStartStandardConfigPacket(ssid, password);
            OutputStream out = socket.getOutputStream();
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.write(buffer, 0, buffer.length);
        }
    }

    class ConnectRunnable implements Runnable {
        private final SetupListener listener;

        public ConnectRunnable(SetupListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                InetAddress tittleAddress = InetAddress.getByName(TITTLE_AP_IP);
                socket.connect(new InetSocketAddress(tittleAddress, TITTLE_AP_PORT), CLIENT_PORT);
                this.listener.onConnected();
            } catch (IOException e) {
                Log.e(TAG, "Error while connecting to Tittle");
                e.printStackTrace();
                cancel(true);
            }
        }
    }

    interface StandardConfigListener {
        void onConfigComplete(String ip);
        void onConfigFailed();
    }
}

interface SetupListener {
    void cancel();

    void onConnected();

    void onSSIDSent();

    void onSSIDReceived();

    void onDeviceInfoReceived(DeviceInfo deviceInfo);

    void onFinishSent();
}

