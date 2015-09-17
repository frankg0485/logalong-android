package com.swoag.logalong.network;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.swoag.logalong.LApp;
import com.swoag.logalong.utils.LBuffer;
import com.swoag.logalong.utils.LBufferPool;
import com.swoag.logalong.utils.LLog;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class LAppServer {
    private static final String TAG = LAppServer.class.getSimpleName();

    private static final String serverIp = "192.168.1.108";
    private static final int serverPort = 1723;

    private Socket socket = null;
    private InputStream sockIn = null;
    private OutputStream sockOut = null;

    private final int STATE_INIT = 10;
    private final int STATE_WAIT = 20;
    private final int STATE_FAIL = 25;
    private final int STATE_READY = 30;
    private final int STATE_OFF = 40;
    private final int STATE_EXIT = 50;

    private Object netLock;
    private int netThreadState;
    private LBufferPool netTxBufPool;
    private LBuffer netRxBuf;

    private static LAppServer instance;
    private boolean connected;
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
    }

    private void closeSockets() {
        try {
            sockOut.close();
        } catch (Exception e) {
        }
        try {
            sockIn.close();
        } catch (Exception e) {
        }

        sockOut = null;
        sockIn = null;
        connected = false;
    }

    public boolean connect() {
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
                                netThreadState = STATE_READY;
                                netLock.notifyAll();
                            }

                            netTxBufPool.enable(true);
                            Thread netThread = new Thread(new NetThread());
                            netThread.start();
                        } catch (Exception e) {
                            LLog.e(TAG, "connection error: " + e.getMessage());
                            connected = false;
                        }
                    }
                });
                connectThread.start();
            } else {
                connected = false;
                LLog.e(TAG, "network not available?");
            }
        }

        return connected;
    }

    public void disconnect() {
        synchronized (netLock) {
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
    }

    private class NetThread implements Runnable {
        public void run() {
            LBuffer buf = null;
            boolean loop = true;
            boolean hasBuf = false;
            boolean fail = false;

            while (loop) {
                synchronized (netLock) {
                    fail = (netThreadState == STATE_FAIL);
                    while (netThreadState != STATE_READY) {
                        if (netThreadState == STATE_OFF) {
                            netThreadState = STATE_EXIT;
                            loop = false;

                            netTxBufPool.flush();
                            closeSockets();
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
                    closeSockets();
                    synchronized (netLock) {
                        netThreadState = STATE_WAIT;
                        netLock.notifyAll();
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
                netLock.notifyAll();
            }
        }
    }

    public LBuffer getNetBuffer() {
        return netTxBufPool.getWriteBufferNeverFail();
    }

    public void putNetBuffer(LBuffer buf) {
        netTxBufPool.putWriteBuffer(buf);
    }

    public void putNetBuffer(LBuffer buf, boolean priority) {
        netTxBufPool.putWriteBuffer(buf, priority);
    }
}
