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

    private static final String serverIp = "192.168.1.108";
    //private static final String serverIp = "162.209.48.52";
    private static final int serverPort = 1723;

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
        if (tried++ >= 3) timeS = AUTO_RECONNECT_DEFAULT_TIME_SECONDS;
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
                                LLog.d(TAG, "open at: " + serverIp + "@" + serverPort);
                                InetAddress serverAddr = InetAddress.getByName(serverIp);
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
                                intent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, LProtocol.RSPS_OK);
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

                if (!fail) {
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
                    } catch (Exception e) {
                        fail = true;
                        LLog.e(TAG, "write error: " + e.getMessage());
                    }
                }

                if (fail) {
                    LLog.w(TAG, "write error");
                } else {
                    try {
                        netRxBuf.setBufOffset(0);
                        netRxBuf.setLen(sockIn.read(netRxBuf.getBuf(), 0, netRxBuf.size()));
                    } catch (Exception e) {
                        fail = true;
                    }

                    if (fail || netRxBuf.getLen() <= 0) {
                        fail = true;
                        LLog.w(TAG, "read error");
                    }
                }

                if (fail) {
                    synchronized (netLock) {
                        closeSockets(AUTO_RECONNECT_RETRY_TIME_SECONDS);
                        //network layer broken in the middle of transfer, teardown server and restart
                        break;
                    }
                } else {
                    lProtocol.parse(netRxBuf);

                    netTxBufPool.putReadBuffer(buf);
                    hasBuf = false;
                }
            }

            // signal the end of connection.
            lProtocol.parse(null);
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
