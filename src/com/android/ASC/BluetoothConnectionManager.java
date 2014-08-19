/*
 * *****************************BLUETOOTHCONNECTIONMANAGER.JAVA****************************
 * This class is responsible for creating, managing and terminating Bluetooth connection with the sensors.
 */

package com.android.ASC;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class BluetoothConnectionManager {
   
    private static final String TAG = "BluetoothConnectionManager";
    private static final String NAME = "ASC";
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter btAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptTask;
    private ConnectThread mConnectTask;
    private ConnectedThread mConnectedTask;
    private int currentState;
    public static final int NONE_STATE = 0;      
    public static final int LISTEN_STATE = 1;     
    public static final int CONNECTING_STATE = 2; 
    public static final int CONNECTED_STATE = 3;  
    
    public BluetoothConnectionManager(Context context, Handler handler) {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        currentState = NONE_STATE;
        mHandler = handler;
    }
   
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + currentState + " -> " + state);
        currentState = state;
        mHandler.obtainMessage(ASCOperator.DATA_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return currentState;
    }
    
    public synchronized void start() {
        Log.d(TAG, "start");
        if (mConnectTask != null) {mConnectTask.cancel(); mConnectTask = null;}
        if (mConnectedTask != null) {mConnectedTask.cancel(); mConnectedTask = null;}
        if (mAcceptTask == null) {
            mAcceptTask = new AcceptThread();
            mAcceptTask.start();
        }
        setState(LISTEN_STATE);
    }
    
    public synchronized void connect(BluetoothDevice device) {        
        if (currentState == CONNECTING_STATE) {
            if (mConnectTask != null) {mConnectTask.cancel(); mConnectTask = null;}
        }
        if (mConnectedTask != null) {mConnectedTask.cancel(); mConnectedTask = null;}
        mConnectTask = new ConnectThread(device);
        mConnectTask.start();
        setState(CONNECTING_STATE);
    }
    
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");
        if (mConnectTask != null) {mConnectTask.cancel(); mConnectTask = null;}
        if (mConnectedTask != null) {mConnectedTask.cancel(); mConnectedTask = null;}
        if (mAcceptTask != null) {mAcceptTask.cancel(); mAcceptTask = null;}
        mConnectedTask = new ConnectedThread(socket);
        mConnectedTask.start();
        Message msg = mHandler.obtainMessage(ASCOperator.DATA_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(ASCOperator.SENSOR_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(CONNECTED_STATE);
    }
    
    public synchronized void stop() {       
        if (mConnectTask != null) {mConnectTask.cancel(); mConnectTask = null;}
        if (mConnectedTask != null) {mConnectedTask.cancel(); mConnectedTask = null;}
        if (mAcceptTask != null) {mAcceptTask.cancel(); mAcceptTask = null;}
        setState(NONE_STATE);
    }
    
    public void write(byte[] out) {       
        ConnectedThread r;        
        synchronized (this) {
            if (currentState != CONNECTED_STATE) return;
            r = mConnectedTask;
        }        
        r.write(out);
    }
    
    private void connectionFailed() {
        setState(LISTEN_STATE);
        Message msg = mHandler.obtainMessage(ASCOperator.DATA_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ASCOperator.TOAST, "Server Socket Opened Successfully...");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    
    private void connectionLost() {
        setState(LISTEN_STATE);       
        Message msg = mHandler.obtainMessage(ASCOperator.DATA_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(ASCOperator.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
   
    private class AcceptThread extends Thread {
       
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;           
            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {           
            setName("AcceptThread");
            BluetoothSocket socket = null;            
            while (currentState != CONNECTED_STATE) {
                try {                   
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }                
                if (socket != null) {
                    synchronized (BluetoothConnectionManager.this) {
                        switch (currentState) {
                        case LISTEN_STATE:
                        case CONNECTING_STATE:                           
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case NONE_STATE:
                        case CONNECTED_STATE:                           
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }            
        }

        public void cancel() {          
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }
   
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;       
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectTask");
            setName("ConnectTask");           
            btAdapter.cancelDiscovery();            
            try {              
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();                
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }               
                BluetoothConnectionManager.this.start();
                return;
            }            
            synchronized (BluetoothConnectionManager.this) {
                mConnectTask = null;
            }           
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        
    }
    
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {            
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;            
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {        
            byte[] buffer = new byte[1024];
            int bytes;           
            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    mHandler.obtainMessage(ASCOperator.DATA_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }
       
        public void write(byte[] buffer) {
            try {
                outStream.write(buffer);

               
                mHandler.obtainMessage(ASCOperator.DATA_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}