package com.swoag.logalong.network;
/* Copyright (C) 2016 SWOAG Technology <www.swoag.com> */


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.swoag.logalong.LApp;
import com.swoag.logalong.utils.LBuffer;
import com.swoag.logalong.utils.LBufferPool;
import com.swoag.logalong.utils.LPreferences;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

public class LRemoteLogging {
    private static final String TAG = LRemoteLogging.class.getSimpleName();
    private static final String serverIp = LAppServer.serverIp;
    private static final int serverPort = 5222;

    private Socket socket = null;
    private InputStream sockIn = null;
    private OutputStream sockOut = null;

    private final int LOGGING_REQUEST_MAGIC = 0xa7cbe9fd;
    public static final short LOGGING_REQUEST_SYNC = (short)0xffaa;

    private final int STATE_INIT = 10;
    private final int STATE_READY = 30;
    private final int STATE_OFF = 40;
    private final int STATE_EXIT = 50;

    private final int MAX_LOG_TX_PACKET_LEN = 2048;
    private final int MAX_LOG_RX_PACKET_LEN = 16;

    private Object netLock;
    private int netThreadState;
    private LBufferPool netTxBufPool;
    private LBuffer netRxBuf;

    private static LRemoteLogging instance;
    private boolean connected;
    private Context context;

    public static LRemoteLogging getInstance() {
        if (null == instance) {
            instance = new LRemoteLogging();
        }
        return instance;
    }

    private LRemoteLogging() {
        context = LApp.ctx;

        connected = false;

        netLock = new Object();
        netThreadState = STATE_INIT;
        netTxBufPool = new LBufferPool(MAX_LOG_TX_PACKET_LEN, 64);
        netTxBufPool.enable(true);

        netRxBuf = new LBuffer(MAX_LOG_RX_PACKET_LEN);
    }

    //caller must hold netLock
    private void closeSockets() {
        try {
            if (null != sockOut) {
                sockOut.close();
            }
        } catch (Exception e) {
        }
        try {
            if (null != sockIn) {
                sockIn.close();
            }
        } catch (Exception e) {
        }

        sockOut = null;
        sockIn = null;
    }

    public boolean connect() {
        synchronized (netLock) {
            if (!connected) {
                connected = true;

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if ((activeNetwork != null) && activeNetwork.isConnectedOrConnecting()) {
                    Thread connectThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (sockIn != null) sockIn.close();
                                if (sockOut != null) sockOut.close();
                                if (socket != null) socket.close();
                            } catch (Exception e) {
                            }

                            try {
                                InetAddress serverAddr = (serverIp.contentEquals("auto")) ?
                                        InetAddress.getByName("swoag.com") : InetAddress.getByName(serverIp);
                                socket = new Socket(serverAddr, serverPort);
                                socket.setSoTimeout(0);

                                sockIn = socket.getInputStream();
                                sockOut = socket.getOutputStream();

                                synchronized (netLock) {
                                    netThreadState = STATE_READY;
                                    netLock.notifyAll();
                                }

                                Thread netThread = new Thread(new NetThread());
                                netThread.start();
                            } catch (Exception e) {
                                synchronized (netLock) {
                                    connected = false;
                                }
                            }
                        }
                    });
                    connectThread.start();
                } else {
                    connected = false;
                }
            }
            return connected;
        }
    }

    public void disconnect() {
        synchronized (netLock) {
            if (netThreadState == STATE_READY) {
                while (netThreadState != STATE_EXIT) {
                    netThreadState = STATE_OFF;
                    netLock.notifyAll();

                    try {
                        netLock.wait();
                    } catch (Exception e) {
                    }
                }
            }
            closeSockets();
            connected = false;
        }
    }

    public boolean isConnected() {
        synchronized (netLock) {
            return connected;
        }
    }

    private class NetThread implements Runnable {

        public void run() {
            LBuffer buf = null;
            boolean loop = true;
            boolean fail = false;

            final int LOG_STATE_DSICONNECTED = 10;
            final int LOG_STATE_CONNECTING = 20;
            final int LOG_STATE_CONNECTED = 30;
            int loggingState = LOG_STATE_DSICONNECTED;
            int id = 0;//LPreferences.getUserId();
            if (0 == id) {
                Random rand = new Random(System.currentTimeMillis());
                id = rand.nextInt(0xffff - 0x8000 + 1) + 0x8000;
            }

            // protocol behaviour:
            // client (App) sends request to server, then wait for response. If response does not
            // come as expected, networks thread bails. Otherwise, keep sending.
            while (loop) {
                synchronized (netLock) {
                    fail = false;
                    while (netThreadState != STATE_READY) {
                        if (netThreadState == STATE_OFF) {
                            netThreadState = STATE_EXIT;
                            loop = false;
                            closeSockets();
                            break;
                        }
                        try {
                            netLock.wait();
                        } catch (Exception e) {
                        }
                    }
                }

                switch (loggingState) {
                    case LOG_STATE_DSICONNECTED:
                        netRxBuf.setBufOffset(0);
                        netRxBuf.putIntAutoInc(LOGGING_REQUEST_MAGIC);
                        netRxBuf.putIntAutoInc(id);
                        netRxBuf.setLen(8);
                        netRxBuf.setBufOffset(0);
                        try {
                            sockOut.write(netRxBuf.getBuf(), 0, netRxBuf.getLen());
                            sockOut.flush();
                            loggingState = LOG_STATE_CONNECTING;
                        } catch (Exception e) {
                            fail = true;
                        }
                        break;

                    case LOG_STATE_CONNECTING:
                        try {
                            netRxBuf.setBufOffset(0);
                            netRxBuf.setLen(sockIn.read(netRxBuf.getBuf(), 0, netRxBuf.size()));
                        } catch (Exception e) {
                            fail = true;
                        }

                        if (fail || netRxBuf.getLen() <= 0) {
                            fail = true;
                        } else if (netRxBuf.getIntAt(0) == id){
                            loggingState = LOG_STATE_CONNECTED;
                        } else
                        {
                            Log.e(TAG, "UNEXPECTED: close socket");
                            fail = true;
                        }
                        break;

                    case LOG_STATE_CONNECTED:
                        buf = netTxBufPool.getReadBufferMayFail();
                        if (buf == null) {
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                            }
                        } else {
                            try {
                                sockOut.write(buf.getBuf(), 0, buf.getLen());
                                sockOut.flush();
                                netTxBufPool.putReadBuffer(buf);
                            } catch (Exception e) {
                                fail = true;
                            }
                        }
                        break;
                }

                if (fail) {
                    synchronized (netLock) {
                        closeSockets();
                        Log.d(TAG, "network layer broken in the middle of transfer, teardown server and restart");
                        break;
                    }
                }
            }

            synchronized (netLock) {
                netThreadState = STATE_EXIT;
                Log.d(TAG, "remote logging thread exit");
                netLock.notifyAll();
            }
        }
    }

    public LBuffer getNetBuffer() {
        synchronized (netLock) {
            //netLock is held before calling to get buffer: getBuffer should NEVER block.
            return netTxBufPool.getWriteBufferMayFail();
        }
    }

    public boolean putNetBuffer(LBuffer buf) {
        synchronized (netLock) {
            netTxBufPool.putWriteBuffer(buf);
            return true;
        }
    }
}
