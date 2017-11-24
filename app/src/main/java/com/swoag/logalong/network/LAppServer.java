package com.swoag.logalong.network;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;

import com.swoag.logalong.BuildConfig;
import com.swoag.logalong.LApp;
import com.swoag.logalong.utils.LAlarm;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LBuffer;
import com.swoag.logalong.utils.LBufferPool;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LAppServer {
    private static final String TAG = LAppServer.class.getSimpleName();

    public static final String serverIp = "192.168.1.116";
    //public static final String serverIp = "auto";
    private static final int serverPort = 8000;

    private Socket socket = null;
    private InputStream sockIn = null;
    private OutputStream sockOut = null;

    private final int STATE_INIT = 10;
    private final int STATE_READY = 30;
    private final int STATE_OFF = 40;
    private final int STATE_EXIT = 50;

    private Object netLock;
    private int netTxThreadState, netRxThreadState;
    private LBufferPool netTxBufPool;
    private LBuffer netRxBuf;

    private static LAppServer instance;
    private boolean connected;
    private int tried;
    private static final int AUTO_RECONNECT_DEFAULT_TIME_SECONDS = 3600 * 2;
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
        lProtocol = LProtocol.getInstance();
        netLock = new Object();
        netRxThreadState = netTxThreadState = STATE_INIT;
        netTxBufPool = new LBufferPool(LProtocol.PACKET_MAX_LEN, 16);
        netTxBufPool.enable(true);

        netRxBuf = new LBuffer(LProtocol.PACKET_MAX_LEN);

        tried = 0;
    }

    private void autoReconnect(int timeS) {
        if (tried++ >= 5) timeS = AUTO_RECONNECT_DEFAULT_TIME_SECONDS;
        LAlarm.cancelAutoReconnectAlarm();
        LAlarm.setAutoReconnectAlarm(System.currentTimeMillis() + timeS * 1000);
    }

    //caller must hold netLock
    private static int INPUT_SOCKET = 0x01;
    private static int OUTPUT_SOCKET = 0x02;

    private void closeSockets(int sock, int timeMs) {
        if (null != sockOut && ((sock & OUTPUT_SOCKET) == OUTPUT_SOCKET)) {
            try {
                sockOut.close();
            } catch (Exception e) {
            }
            sockOut = null;
        }

        if (null != sockIn && ((sock & INPUT_SOCKET) == INPUT_SOCKET)) {
            try {
                sockIn.close();
            } catch (Exception e) {
            }
            sockIn = null;
        }

        if (sockIn == null && sockOut == null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
            socket = null;

            connected = false;
            autoReconnect(timeMs);
        }
    }

    public boolean connect() {
        synchronized (netLock) {
            if (!connected) {
                connected = true;
                LLog.d(TAG, "connection startup ...");
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if ((activeNetwork != null) && activeNetwork.isConnectedOrConnecting()) {
                    Thread connectThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (sockIn != null) sockIn.close();
                                if (sockOut != null) sockOut.close();
                                if (socket != null) {
                                    socket.close();
                                    LLog.w(TAG, "unexpected: socket was not properly shut down");
                                }
                            } catch (Exception e) {
                            }

                            try {
                                InetAddress serverAddr = (serverIp.contentEquals("auto")) ?
                                        InetAddress.getByName("swoag.com") : InetAddress.getByName(serverIp);
                                LLog.d(TAG, "server addr:" + serverAddr);
                                socket = new Socket(serverAddr, serverPort);
                                LLog.d(TAG, "stream opened: " + socket);
                                socket.setSoTimeout(3000);

                                sockIn = socket.getInputStream();
                                sockOut = socket.getOutputStream();

                                synchronized (netLock) {
                                    netTxBufPool.enable(true);
                                    netTxBufPool.flush();

                                    netTxThreadState = netRxThreadState = STATE_READY;
                                    netLock.notifyAll();
                                }

                                new Thread(new NetTxThread()).start();
                                new Thread(new NetRxThread()).start();

                                tried = 0;
                                LLog.d(TAG, "network connected");
                                Intent intent;
                                intent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                        .ACTION_NETWORK_CONNECTED));
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
            } else
                LLog.d(TAG, "server already connected");
            return connected;
        }
    }

    public void disconnect() {
        synchronized (netLock) {
            if (connected) {
                if (netRxThreadState == STATE_READY) {
                    while (netRxThreadState != STATE_EXIT) {
                        netRxThreadState = STATE_OFF;
                        netLock.notifyAll();

                        try {
                            netLock.wait(5000);
                        } catch (Exception e) {
                        }
                    }
                }

                if (netTxThreadState == STATE_READY) {
                    while (netTxThreadState != STATE_EXIT) {
                        netTxThreadState = STATE_OFF;
                        netTxBufPool.enable(false);
                        netLock.notifyAll();

                        try {
                            netLock.wait(5000);
                        } catch (Exception e) {
                        }
                    }
                }
            }
            closeSockets(INPUT_SOCKET | OUTPUT_SOCKET, AUTO_RECONNECT_DEFAULT_TIME_SECONDS);
        }
    }

    private class NetTxThread implements Runnable {

        public void run() {
            boolean loop = true;
            boolean haveBuffer = false;
            LBuffer buf = null;

            LLog.d(TAG, "net tx thread running ...");
            while (loop) {
                synchronized (netLock) {
                    if (netTxThreadState == STATE_OFF) {
                        break;
                    }
                }

                //LLog.d(TAG, "net tx thread get buffer");
                if (!haveBuffer) {
                    buf = netTxBufPool.getReadBuffer();
                    if (buf == null) {
                        LLog.w(TAG, "network thread: interrupted unable to get read buffer");
                        continue;
                    } else {
                        haveBuffer = true;
                    }
                }

                //LLog.d(TAG, "net tx thread sending ... ");
                try {
                    sockOut.write(buf.getBuf(), 0, buf.getLen());
                    sockOut.flush();
                } catch (SocketTimeoutException e) {
                    LLog.d(TAG, "net write timeout exception: " + e.getMessage());
                } catch (Exception e) {
                    loop = false;
                    LLog.e(TAG, "net write exception: " + e.getMessage());
                }

                netTxBufPool.putReadBuffer(buf);
                haveBuffer = false;
                //LLog.d(TAG, "net tx thread sending done");
            }

            LLog.d(TAG, "app server stopped: net tx thread done");
            synchronized (netLock) {
                if (netRxThreadState == STATE_READY) netRxThreadState = STATE_OFF;

                closeSockets(OUTPUT_SOCKET, AUTO_RECONNECT_RETRY_TIME_SECONDS);
                netTxThreadState = STATE_EXIT;
                netLock.notifyAll();
            }

            Intent intent;
            intent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_NETWORK_DISCONNECTED));
            LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(intent);
        }
    }


    private class NetRxThread implements Runnable {

        public void run() {
            boolean loop = true;

            LLog.d(TAG, "net rx thread running ...");
            while (loop) {
                synchronized (netLock) {
                    if (netRxThreadState == STATE_OFF) {
                        break;
                    }
                }
                //LLog.d(TAG, "rx thread receiving ...");
                try {
                    netRxBuf.setBufOffset(0);
                    netRxBuf.setLen(sockIn.read(netRxBuf.getBuf(), 0, netRxBuf.size()));
                    if (netRxBuf.getLen() <= 0) {
                        Thread.sleep(1000);
                        LLog.w(TAG, "read error");
                        continue;
                    }
                } catch (SocketTimeoutException e) {
                    LLog.d(TAG, "net read timeout exception: " + e.getMessage());
                } catch (Exception e) {
                    LLog.d(TAG, "net read exception: " + e.getMessage());
                    break;
                }

                //LLog.d(TAG, "rx thread receiving returned");
                lProtocol.parse(netRxBuf, scrambler);
            }

            // signal the end of connection.
            LLog.d(TAG, "app server stop: net rx thread done");
            lProtocol.shutdown();
            synchronized (netLock) {
                if (netTxThreadState == STATE_READY) {
                    netTxThreadState = STATE_OFF;
                    netTxBufPool.enable(false);
                }

                closeSockets(INPUT_SOCKET, AUTO_RECONNECT_RETRY_TIME_SECONDS);
                netRxThreadState = STATE_EXIT;
                netLock.notifyAll();
            }

            Intent intent;
            intent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_NETWORK_DISCONNECTED));
            LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(intent);
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

    //all user interface calls
    private int scrambler;

    private int genScrambler() {
        Random rand = new Random(System.currentTimeMillis());
        int ss = 0;
        int ii = 0;
        while (ii < 4) {
            char ch = (char) (rand.nextInt(74) + 48);
            if ((ch > 'Z' && ch < 'a') || (ch > '9' && ch < 'A')) continue;
            ii++;
            ss <<= 8;
            ss += ch;
        }

        return ss;
    }

    public boolean UiIsConnected() {
        return lProtocol.isConnected();
    }

    public boolean UiIsLoggedIn() {
        return lProtocol.isLoggedIn();
    }

    public short UiGetServerVersion() {
        return lProtocol.getServerVersion();
    }

    public void UiInitScrambler() {
        scrambler = genScrambler();
        LTransport.send_rqst(this, LProtocol.RQST_SCRAMBLER_SEED, scrambler, (short) BuildConfig.VERSION_CODE,
                (short) 1, 0);
    }

    public boolean UiPing() {
        return LTransport.send_rqst(this, LProtocol.RQST_PING, 0);
    }

    public boolean UiGetUserByName(String name) {
        return LTransport.send_rqst(this, LProtocol.RQST_GET_USER_BY_NAME, name, scrambler);
    }

    public boolean UiCreateUser(String name, String pass, String fullName) {
        List<String> strings = new ArrayList<>();
        strings.add(name);
        strings.add(pass);
        strings.add(fullName);
        return LTransport.send_rqst(this, LProtocol.RQST_CREATE_USER, strings, scrambler);
    }

    public boolean UiSignIn(String name, String pass) {
        List<String> strings = new ArrayList<>();
        strings.add(name);
        strings.add(pass);
        return LTransport.send_rqst(this, LProtocol.RQST_SIGN_IN, strings, scrambler);
    }

    public boolean UiUpdateUserProfile(String name, String pass, String newPass, String fullName) {
        List<String> strings = new ArrayList<>();
        strings.add(name);
        strings.add(pass);
        strings.add(newPass);
        strings.add(fullName);
        return LTransport.send_rqst(this, LProtocol.RQST_UPDATE_USER_PROFILE, strings, scrambler);
    }

    public boolean UiLogIn(String name, String pass) {
        List<String> strings = new ArrayList<>();
        strings.add(name);
        strings.add(pass);
        strings.add(LPreferences.getDeviceId());
        return LTransport.send_rqst(this, LProtocol.RQST_LOG_IN, strings, scrambler);
    }

    public boolean UiResetPassword(String name, String email) {
        List<String> strings = new ArrayList<>();
        strings.add(name);
        strings.add(email);
        return LTransport.send_rqst(this, LProtocol.RQST_RESET_PASSWORD, strings, scrambler);
    }

    public boolean UiPoll() {
        return LTransport.send_rqst(this, LProtocol.RQST_POLL, scrambler);
    }

    public boolean UiPollAck(long id) {
        return LTransport.send_rqst(this, LProtocol.RQST_POLL_ACK, id, scrambler);
    }

    public boolean UiUtcSync() {
        return LTransport.send_rqst(this, LProtocol.RQST_UTC_SYNC, System.currentTimeMillis() / 1000, scrambler);
    }

    public boolean UiPostJournal(int journalId, byte[] data) {
        return LTransport.send_rqst(this, LProtocol.RQST_POST_JOURNAL, journalId, data, scrambler);

        /*
        short maxPayloadLen = LProtocol.PACKET_MAX_PAYLOAD_LEN - 2 - 4 - 4 - 2;
        //DATA_LENGTH = PAYLOAD_LENGTH - REQUEST_CODE - USER_ID - JOURNAL_ID - DATA_LENGTH_FIELD
        if (data.length <= maxPayloadLen) {
            return LTransport.send_rqst(this, LProtocol.RQST_POST_JOURNAL, userId, journalId, (short)data.length,
            data, 0, (short)data.length, scrambler);
        } else {
            //jumbbo journal, split the transmit
            int offset = 0;
            short bytes = maxPayloadLen;
            short length = (short)data.length;
            int totalBytes = data.length;

            do {
                if (!LTransport.send_rqst(this, LProtocol.RQST_POST_JOURNAL, userId, journalId, length, data, offset,
                 bytes, scrambler)) return false;

                offset += bytes;
                totalBytes -= bytes;
                length = bytes = (totalBytes > maxPayloadLen) ? maxPayloadLen : (short) totalBytes;
            } while (bytes > 0);
            return true;
        }*/
    }
}
