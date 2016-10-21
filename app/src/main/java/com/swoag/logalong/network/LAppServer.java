package com.swoag.logalong.network;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;

import com.swoag.logalong.LApp;
import com.swoag.logalong.utils.LAlarm;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LBuffer;
import com.swoag.logalong.utils.LBufferPool;
import com.swoag.logalong.utils.LLog;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Calendar;

public class LAppServer {
    private static final String TAG = LAppServer.class.getSimpleName();

    //public static final String serverIp = "192.168.1.149";
    public static final String serverIp = "auto";
    private static final int serverPort = 8000;

    private Socket socket = null;
    private InputStream sockIn = null;
    private OutputStream sockOut = null;

    private final int STATE_INIT = 10;
    private final int STATE_READY = 30;
    private final int STATE_OFF = 40;
    private final int STATE_EXIT = 50;

    private Object netLock;
    private int netThreadState;
    private LBufferPool netTxBufPool;
    private LBuffer netRxBuf;

    private static LAppServer instance;
    private boolean connected;
    private int tried;
    private static final int AUTO_RECONNECT_DEFAULT_TIME_SECONDS = 3600;
    private static final int AUTO_RECONNECT_RETRY_TIME_SECONDS = 30;

    private Context context;
    private LProtocol lProtocol;

    public static LAppServer getInstance() {
        if (null == instance) {
            instance = new LAppServer();
        }
        return instance;
    }

    private LAppServer() {
        context = LApp.ctx;
        lProtocol = new LProtocol();
        netLock = new Object();
        netThreadState = STATE_INIT;
        netTxBufPool = new LBufferPool(LProtocol.PACKET_MAX_LEN, 16);
        netTxBufPool.enable(true);

        netRxBuf = new LBuffer(LProtocol.PACKET_MAX_LEN);

        tried = 0;
    }

    private void autoReconnect(int timeS) {
        if (tried++ >= 5) timeS = AUTO_RECONNECT_DEFAULT_TIME_SECONDS;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(calendar.getTimeInMillis() + timeS * 1000);
        LAlarm.cancelAutoReconnectAlarm();
        LAlarm.setAutoReconnectAlarm(calendar.getTimeInMillis());
    }

    //caller must hold netLock
    private void closeSockets(int timeMs) {
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
        connected = false;
        autoReconnect(timeMs);
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
                                LLog.d(TAG, "server addr:" + serverAddr);
                                socket = new Socket(serverAddr, serverPort);
                                LLog.d(TAG, "stream opened: " + socket);
                                socket.setSoTimeout(0);

                                sockIn = socket.getInputStream();
                                sockOut = socket.getOutputStream();

                                synchronized (netLock) {
                                    netTxBufPool.enable(true);
                                    netTxBufPool.flush();

                                    netThreadState = STATE_READY;
                                    netLock.notifyAll();
                                }

                                Thread netThread = new Thread(new NetThread());
                                netThread.start();
                                tried = 0;

                                Intent intent;
                                intent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_NETWORK_CONNECTED));
                                intent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, (int) LProtocol.RSPS_OK);
                                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(intent);
                            } catch (Exception e) {
                                LLog.e(TAG, "connection error: " + e.getMessage());
                                synchronized (netLock) {
                                    connected = false;
                                    autoReconnect(AUTO_RECONNECT_RETRY_TIME_SECONDS);
                                }
                            }
                        }
                    });
                    connectThread.start();
                } else {
                    connected = false;
                    autoReconnect(AUTO_RECONNECT_RETRY_TIME_SECONDS);
                    LLog.e(TAG, "network not available?");
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
                    netTxBufPool.enable(false);
                    netLock.notifyAll();

                    try {
                        netLock.wait();
                    } catch (Exception e) {
                    }
                }
            }
            closeSockets(AUTO_RECONNECT_DEFAULT_TIME_SECONDS);
        }
    }

    private class NetThread implements Runnable {

        public void run() {
            LBuffer buf = null;
            boolean loop = true;
            boolean hasBuf = false;
            boolean fail = false;

            final int SEND_REQUEST = 10;
            final int WAIT_FOR_RESPONSE = 20;

            int protocolState = SEND_REQUEST;
            short requestCode = 0;
            // protocol behaviour:
            // client (App) sends request to server, then wait for response. If response does not
            // come as expected, networks thread bails to restart.
            while (loop) {
                synchronized (netLock) {
                    fail = false;
                    while (netThreadState != STATE_READY) {
                        if (netThreadState == STATE_OFF) {
                            netThreadState = STATE_EXIT;
                            loop = false;
                            closeSockets(AUTO_RECONNECT_DEFAULT_TIME_SECONDS);
                            break;
                        }
                        try {
                            netLock.wait();
                        } catch (Exception e) {
                        }
                    }
                }

                switch (protocolState) {
                    case SEND_REQUEST:
                        if (!hasBuf) {
                            buf = netTxBufPool.getReadBuffer();
                            if (buf == null) {
                                LLog.w(TAG, "network thread: interrupted unable to get read buffer");
                                continue;
                            } else
                                hasBuf = true;
                        }

                        try {
                            sockOut.write(buf.getBuf(), 0, buf.getLen());
                            sockOut.flush();
                            requestCode = buf.getShortAt(4);
                        } catch (Exception e) {
                            fail = true;
                            LLog.e(TAG, "write error: " + e.getMessage());
                            break;
                        }

                        protocolState = WAIT_FOR_RESPONSE;
                        //fall through

                    case WAIT_FOR_RESPONSE:
                        try {
                            netRxBuf.setBufOffset(0);
                            netRxBuf.setLen(sockIn.read(netRxBuf.getBuf(), 0, netRxBuf.size()));
                        } catch (Exception e) {
                            fail = true;
                        }

                        if (fail || netRxBuf.getLen() <= 0) {
                            fail = true;
                            LLog.w(TAG, "read error");
                        } else {
                            int parseResult = lProtocol.parse(netRxBuf, requestCode);
                            switch (parseResult) {
                                case LProtocol.RESPONSE_PARSE_RESULT_DONE :
                                    netTxBufPool.putReadBuffer(buf);
                                    hasBuf = false;
                                    protocolState = SEND_REQUEST;
                                    break;
                                case LProtocol.RESPONSE_PARSE_RESULT_MORE2COME :
                                    //continue to read more
                                    break;
                                case LProtocol.RESPONSE_PARSE_RESULT_ERROR :
                                    LLog.e(TAG, "protocol broken, restart service");
                                    fail = true;
                                    break;
                            }
                        }
                        break;
                }

                if (fail) {
                    synchronized (netLock) {
                        closeSockets(AUTO_RECONNECT_RETRY_TIME_SECONDS);
                        //network layer broken in the middle of transfer, teardown server and restart
                        break;
                    }
                }
            }

            // signal the end of connection.
            LLog.d(TAG, "app server stopped");
            lProtocol.parse(null, (short)0);
            synchronized (netLock) {
                netThreadState = STATE_EXIT;
                netLock.notifyAll();
            }
        }
    }

    public LBuffer getNetBuffer() {
        synchronized (netLock) {
            if (!connected) return null;
            //netLock is held before calling to get buffer: getBuffer should NEVER block.
            return netTxBufPool.getWriteBufferNeverFail();
        }
    }

    public boolean putNetBuffer(LBuffer buf) {
        synchronized (netLock) {
            if (!connected) return false;
            netTxBufPool.putWriteBuffer(buf);
            return true;
        }
    }
}
