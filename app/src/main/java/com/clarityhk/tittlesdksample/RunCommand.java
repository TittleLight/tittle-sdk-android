package com.clarityhk.tittlesdksample;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

class RunCommand implements Runnable {
    private static final String TAG = RunCommand.class.getSimpleName();

    private final byte[] command;
    private final Connection connection;
    private final CommandListener listener;

    private Thread commandResponseThread;

    public RunCommand(Connection connection, byte[] command, CommandListener listener) {
        this.connection = connection;
        this.command = command;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            connection.connect();
            commandResponseThread = new Thread(new CommandResponseRunnable(connection, this.listener));
            commandResponseThread.start();
            new SendCommandRunnable(connection, command).run();
        } catch (IOException e) {
            e.printStackTrace();
            listener.commandFailed();
        }
    }

    private void cancel() {
        if (commandResponseThread != null && commandResponseThread.isAlive())
            commandResponseThread.interrupt();
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
            OutputStream out = null;
            DataOutputStream dataOut = null;
            try {
                out = connection.socket.getOutputStream();
                dataOut = new DataOutputStream(out);
                dataOut.write(command, 0, command.length);
                dataOut.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error while sending command " + Util.toHexString(command));
                e.printStackTrace();
                listener.commandFailed();
                cancel();
            }
        }
    }

    class CommandResponseRunnable implements Runnable {
        private final Connection connection;
        private final CommandListener listener;

        CommandResponseRunnable(Connection connection, CommandListener listener) {
            this.connection = connection;
            this.listener = listener;
        }

        @Override
        public void run() {
            Log.d(TAG, "Listening for command response");
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
                Log.d(TAG, "Error while listening for command response");
                e.printStackTrace();
                listener.commandFailed();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    interface CommandListener {
        void onResponseReceived(byte[] data);

        void commandFailed();
    }
}
