package com.example.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService {

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public static final int MSG_STATE_CHANGE = 1;
    public static final int MSG_READ = 2;
    public static final int MSG_WRITE = 3;
    public static final int MSG_DEVICE_NAME = 4;
    public static final int MSG_TOAST = 5;

    private static final String APP_NAME = "BluetoothChat";
    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler handler;
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;

    public BluetoothService(Handler handler) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.handler = handler;
        this.state = STATE_NONE;
    }

    private synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MSG_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() { return state; }

    public synchronized void start() {
        if (connectThread != null) { connectThread.cancel(); connectThread = null; }
        if (connectedThread != null) { connectedThread.cancel(); connectedThread = null; }
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device) {
        if (connectThread != null) { connectThread.cancel(); connectThread = null; }
        if (connectedThread != null) { connectedThread.cancel(); connectedThread = null; }
        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectThread != null) { connectThread.cancel(); connectThread = null; }
        if (connectedThread != null) { connectedThread.cancel(); connectedThread = null; }
        if (acceptThread != null) { acceptThread.cancel(); acceptThread = null; }
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        android.os.Message msg = handler.obtainMessage(MSG_DEVICE_NAME);
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("device_name", device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (connectThread != null) { connectThread.cancel(); connectThread = null; }
        if (connectedThread != null) { connectedThread.cancel(); connectedThread = null; }
        if (acceptThread != null) { acceptThread.cancel(); acceptThread = null; }
        setState(STATE_NONE);
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }
        r.write(out);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) { e.printStackTrace(); }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;
            while (state != STATE_CONNECTED) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) { break; }
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try { serverSocket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { e.printStackTrace(); }
            socket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
            } catch (IOException e) {
                try { socket.close(); } catch (IOException e2) { e2.printStackTrace(); }
                connectionFailed();
                return;
            }
            synchronized (BluetoothService.this) { connectThread = null; }
            connected(socket, device);
        }

        public void cancel() {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { e.printStackTrace(); }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(MSG_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                handler.obtainMessage(MSG_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) { e.printStackTrace(); }
        }

        public void cancel() {
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void connectionFailed() {
        android.os.Message msg = handler.obtainMessage(MSG_TOAST);
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("toast", "Unable to connect to device");
        msg.setData(bundle);
        handler.sendMessage(msg);
        BluetoothService.this.start();
    }

    private void connectionLost() {
        android.os.Message msg = handler.obtainMessage(MSG_TOAST);
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("toast", "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);
        BluetoothService.this.start();
    }
}