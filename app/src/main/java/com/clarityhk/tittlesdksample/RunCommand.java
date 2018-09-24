package com.clarityhk.tittlesdksample;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

class RunCommand implements Runnable, CommandResponseListener {
    private static final String TAG = RunCommand.class.getSimpleName();

    private final byte[] command;
    private final Connection connection;

    public RunCommand(Connection connection, byte[] command) {
        this.connection = connection;
        this.command = command;
    }

    @Override
    public void onResponseReceived(byte[] data) {
        if (data == null) {
            Log.d(TAG, "Empty response received");
        } else {
            Log.d(TAG, "Response received: " + Util.toHexString(data));
        }
    }

    @Override
    public void run() {
        try {
            connection.connect();
            new Thread(new CommandResponseRunnable(connection, this)).start();
            new SendCommandRunnable(connection, command).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class SendCommandRunnable implements Runnable {
        private final byte[] command;
        private final Connection connection;

        SendCommandRunnable(Connection connection, byte[] command) {
            this.connection = connection;
            this.command = command;
        }

        @Override
        public void run() {
            Log.d(TAG, "Sending command");
            try {
                OutputStream out = connection.socket.getOutputStream();
                DataOutputStream dataOut = new DataOutputStream(out);
                dataOut.write(command, 0, command.length);
            } catch (IOException e) {
                Log.e(TAG, "Error while sending command " + Util.toHexString(command));
                e.printStackTrace();
            }
        }
    }

    class CommandResponseRunnable implements Runnable {
        private final Connection connection;
        private final CommandResponseListener listener;

        CommandResponseRunnable(Connection connection, CommandResponseListener listener) {
            this.connection = connection;
            this.listener = listener;
        }

        @Override
        public void run() {
            Log.d(TAG, "Listening for response");
            InputStream input = null;
            try {
                input = connection.socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead = input.read(buffer);
                if (bytesRead != -1) {
                    byte[] result = Arrays.copyOf(buffer, bytesRead);
                    listener.onResponseReceived(result);
                } else {
                    listener.onResponseReceived(null);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
