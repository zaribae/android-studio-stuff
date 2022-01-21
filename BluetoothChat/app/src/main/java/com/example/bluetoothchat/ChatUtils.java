package com.example.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ChatUtils {
    private Context context;
    private final Handler handler;
    private int state;
    private ConnectThread connectThread;
    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;

    private final UUID APP_UUID = UUID.fromString("b5b87d4c-7abe-11ec-90d6-0242ac120003");
    private final String APP_NAME = "Bluetooth Chat App";

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public int getState() {
        return state;
    }

    private synchronized void start() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        setState(STATE_LISTEN);
    }

    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        setState(STATE_NONE);
    }

    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();

        setState(STATE_CONNECTING);
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e("Accept->Constructor", e.toString());
            }

            serverSocket =  tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e("Accept->Run", e.toString());
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    Log.e("Accept->Close", e.toString());
                }
            }

            if (socket != null) {
                switch (state) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connect(socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e("Accept->CloseSocket", e.toString());
                        }
                        break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("Accept->CloseServer", e.toString());
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }

            socket = tmp;
        }

        public void run() {
            try {
                socket.connect();
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("Connect->CloseSocket", e.toString());
                }
            }

            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            connect(device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }
    }

    private synchronized void connectionFailed() {
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Can't connect to the device" );
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private synchronized void connected(BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
