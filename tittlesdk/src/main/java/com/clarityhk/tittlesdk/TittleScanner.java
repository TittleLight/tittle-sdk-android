package com.clarityhk.tittlesdk;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

/*
    Scan for Tittles in your current network. Your phone should broadcast packets over UDP to which
    (all) Tittle devices in your network will respond to (over TCP). The response contains Tittles
    name, IP and Mac address.
 */
public class TittleScanner {
    private final static String TAG = TittleScanner.class.getSimpleName();
    private static final int BROADCAST_PORT = 10000;
    private static final int TCP_SERVER_PORT = 9999;
    private final InetAddress handsetIp;

    private Thread tcpReceiverThread;
    private Thread broadcastRunnableThread;

    private ServerSocket serverSocket;
    private DatagramSocket broadcastSocket;

    public TittleScanner(InetAddress handsetIp) {
        this.handsetIp = handsetIp;
    }

    public void scan(int timeout, TittleScanListener listener) {
        try {
            tcpReceiverThread = new Thread(new TCPReceiver(timeout, listener));
            tcpReceiverThread.start();
            broadcastRunnableThread = new Thread(new BroadcastRunnable(this.handsetIp, timeout, listener));
            broadcastRunnableThread.start();
        } catch (IOException e) {
            Log.d(TAG, "Failed to start Tittle scan");
            e.printStackTrace();
        }
    }


    public void cancel() {
        closeServerSocket();
        closeBroadcastSocket();
        if (tcpReceiverThread != null) tcpReceiverThread.interrupt();
        if (broadcastRunnableThread != null) broadcastRunnableThread.interrupt();
    }


    class BroadcastRunnable implements Runnable {
        private final InetAddress handsetIp;
        private final int timeout;
        private final TittleScanListener listener;

        public BroadcastRunnable(InetAddress handsetIp, int timeout, TittleScanListener listener) {
            this.handsetIp = handsetIp;
            this.timeout = timeout;
            this.listener = listener;
        }

        @Override
        public void run() {
            Log.d(TAG, "Started sending scan requests...");
            long startedAt = System.currentTimeMillis();
            try {
                final byte[] data = TittleCommands.createScanBroadcastPacket(this.handsetIp);
                InetAddress broadcastAddress = Util.getBroadcastAddress(this.handsetIp);
                broadcastSocket = new DatagramSocket();
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length, broadcastAddress, BROADCAST_PORT);
                broadcastSocket.setBroadcast(true);
                while (!Thread.currentThread().isInterrupted() && startedAt + this.timeout > System.currentTimeMillis()) {
                    broadcastSocket.send(datagramPacket);
                    Log.d(TAG, "Scan sent");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            closeBroadcastSocket();
            Log.d(TAG, "Stopped broadcasting scan requests");
            if (!tcpReceiverThread.isAlive()) listener.scanComplete();
        }
    }

    class TCPReceiver implements Runnable {
        final int timeout;
        final TittleScanListener listener;

        public TCPReceiver(int timeout, TittleScanListener listener) throws IOException {
            this.timeout = timeout;
            this.listener = listener;
            serverSocket = new ServerSocket();
            serverSocket.setSoTimeout((int) Math.floor(timeout * 1.1));
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(TCP_SERVER_PORT));
        }

        @Override
        public void run() {
            Log.d(TAG, "Started listening to scan responses");
            long startedAt = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted() && serverSocket != null && serverSocket.isBound() && !serverSocket.isClosed() && startedAt + this.timeout > System.currentTimeMillis()) {
                try {
                    Socket socket = serverSocket.accept();
                    InputStream input = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead = input.read(buffer);
                    if (bytesRead != -1) {
                        byte response[] = Arrays.copyOfRange(buffer, 0, bytesRead);
                        try {
                            DeviceInfo deviceInfo = TittleCommands.parseDeviceInfo(response, new byte[] { 0x70, 0x04 });
                            listener.onTittleFound(deviceInfo);
                        } catch (MalformedResponseException e) {
                            Log.d(TAG, "Received malformed response to Tittle scan: " + Util.toHexString(response));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            closeServerSocket();
            Log.d(TAG, "Stopped listening to scan responses");
            if (!broadcastRunnableThread.isAlive()) listener.scanComplete();
        }
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeBroadcastSocket() {
        if (broadcastSocket != null && !broadcastSocket.isClosed()) broadcastSocket.close();
    }

    public interface TittleScanListener {
        void onTittleFound(DeviceInfo deviceInfo);
        void scanComplete();
    }
}
