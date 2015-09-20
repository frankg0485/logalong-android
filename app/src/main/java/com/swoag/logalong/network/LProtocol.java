package com.swoag.logalong.network;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.swoag.logalong.LApp;
import com.swoag.logalong.utils.AppPersistency;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LBuffer;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;

import java.util.Random;

public class LProtocol {
    private static final String TAG = LProtocol.class.getSimpleName();
    private static int scrambler;
    private static boolean connected;

    //////////////////////////////////////////////////////////////////////
    public static final int PACKET_MAX_PAYLOAD_LEN = 1456;
    public static final int PACKET_MAX_LEN = (PACKET_MAX_PAYLOAD_LEN + 4);

    public static final short PACKET_SIGNATURE1 = (short) 0xffaa;

    private static final short PAYLOAD_DIRECTION_RQST = 0;
    private static final short PAYLOAD_DIRECTION_RSPS = (short) 0x8000;

    private static final short PAYLOAD_TYPE_MASK = 0x1800;
    private static final short PAYLOAD_TYPE_SHIFT = 11;
    private static final short PAYLOAD_VALUE_MASK = 0x07ff;

    public static final short PLAYLOAD_TYPE_SYS = (short) (2 << PAYLOAD_TYPE_SHIFT);
    public static final short PLAYLOAD_TYPE_USER = (short) (3 << PAYLOAD_TYPE_SHIFT);

    public static int PACKET_PAYLOAD_LENGTH(int payloadLen) {
        return ((((payloadLen) + 3) / 4) * 4);
    }

    private static final short RQST_SYS = PLAYLOAD_TYPE_SYS | PAYLOAD_DIRECTION_RQST;
    private static final short RQST_USER = PLAYLOAD_TYPE_USER | PAYLOAD_DIRECTION_RQST;
    private static final short RSPS = PAYLOAD_DIRECTION_RSPS;

    private static final short RSPS_OK = (short) 0x0010;
    private static final short RSPS_ERROR = (short) 0xffff;

    private static final short RQST_SCRAMBLER_SEED = RQST_SYS | 0x100;
    private static final short RQST_CREATE_USER = RQST_SYS | 0x104;
    private static final short RQST_GET_SHARE_USER_BY_ID = RQST_SYS | 0x108;
    private static final short RQST_GET_SHARE_USER_BY_NAME = RQST_SYS | 0x109;
    private static final short RQST_PING = RQST_SYS | 0x7ff;


    private LBuffer pktBuf;
    private LBuffer pkt;
    private short[] shorts;

    public LProtocol() {
        pktBuf = new LBuffer(PACKET_MAX_LEN * 2);
        shorts = new short[PACKET_MAX_LEN];
    }

    private int consumePacket(LBuffer pkt) {
        Intent rspsIntent;
        int origOffset = pkt.getBufOffset();
        short total = pkt.getShortAt(origOffset + 2);
        short rsps = pkt.getShortAt(origOffset + 4);
        short status;

        switch (rsps) {
            case RSPS | RQST_PING:
                LLog.d(TAG, "pong");
                break;

            case RSPS | RQST_SCRAMBLER_SEED:
                LLog.d(TAG, "channel scrambler seed sent");
                connected = true;
                break;

            case RSPS | RQST_CREATE_USER:
                LTransport.scramble(pkt, scrambler);
                pkt.skip(6);
                status = pkt.getShortAutoInc();
                if (status == RSPS_OK) {
                    int userId = pkt.getIntAutoInc();
                    short bytes = pkt.getShortAutoInc();
                    String userName = pkt.getStringAutoInc(bytes);
                    LLog.d(TAG, "user created as: " + userName);

                    LPreferences.setUserId(userId);
                    LPreferences.setUserName(userName);
                } else {
                    LLog.w(TAG, "unable to create user");
                }
                break;
            case RSPS | RQST_GET_SHARE_USER_BY_ID:
                LTransport.scramble(pkt, scrambler);
                pkt.skip(6);
                status = pkt.getShortAutoInc();
                rspsIntent = new Intent(LBroadcastReceiver.ACTION_GET_SHARE_USER_BY_ID);
                rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                if (status == RSPS_OK) {
                    int userId = pkt.getIntAutoInc();
                    short bytes = pkt.getShortAutoInc();
                    String userName = pkt.getStringAutoInc(bytes);
                    rspsIntent.putExtra("id", userId);
                    rspsIntent.putExtra("name", userName);
                    LLog.d(TAG, "user returned as: " + userName);
                }
                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                break;

            case RSPS | RQST_GET_SHARE_USER_BY_NAME:
                LTransport.scramble(pkt, scrambler);
                pkt.skip(6);
                status = pkt.getShortAutoInc();
                rspsIntent = new Intent(LBroadcastReceiver.ACTION_GET_SHARE_USER_BY_NAME);
                if (status == RSPS_OK) {
                    rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, (int)0);
                    int userId = pkt.getIntAutoInc();
                    short bytes = pkt.getShortAutoInc();
                    String userName = pkt.getStringAutoInc(bytes);
                    rspsIntent.putExtra("id", userId);
                    rspsIntent.putExtra("name", userName);
                    LLog.d(TAG, "user returned as: " + userName);
                } else {
                    rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, (int)status);
                }
                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                break;
        }

        pkt.setBufOffset(origOffset);
        return total;
    }

    private boolean alignPacket(LBuffer pkt) {
        while (pkt.getLen() >= 8) {
            short sig = pkt.getShort();
            if (sig != PACKET_SIGNATURE1) {
                LLog.w(TAG, String.format("packet misaligned: %x", sig));
                pkt.skip(1);
                pkt.modLen(-1);
            } else {
                if (pkt.getLen() >= 8) return true;
            }
        }
        return false;
    }

    // parser runs in Network receiving thread, thus no GUI update here.
    public void parse(LBuffer buf) {
        if (null == buf) {
            connected = false;
            return;
        }

        if (pktBuf.getLen() > 0) {
            LLog.d(TAG, "packet pipe fragmented");
            pktBuf.append(buf);
            pkt = pktBuf;
        } else {
            pkt = buf;
        }

        while (alignPacket(pkt)) {
            int bytes = consumePacket(pkt);
            if (bytes == -1) {
                //TODO: packet or state error??
                LLog.e(TAG, "packet parse error?");
            } else if (bytes == 0) {
                //packet not consumed
                if (pkt != pktBuf) {
                    pktBuf.append(pkt);
                    return;
                } else {
                    //this must be the case where the data is only partially received.
                    //quit the loop
                    return;
                }
            } else {
                //packet consumed
                pkt.setBufOffset(pkt.getBufOffset() + bytes);
                pkt.setLen(pkt.getLen() - bytes);
                //LLog.d(TAG, "continue parsing: " + buf.getLen() + " offset: " + pkt.getBufOffset());
                //LLog.d(TAG, LLog.bytesToHex(pkt.getBuf()));
            }
        }
    }

    // all user interface calls
    public static class ui {
        private static LAppServer server;

        private static int genScrambler() {
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

        public static void connect() {
            if (!connected) {
                server = LAppServer.getInstance();
                server.connect();

                scrambler = genScrambler();
                LTransport.send_rqst(server, RQST_SCRAMBLER_SEED, scrambler, 0);
            }
        }

        public static void disconnect() {
            if (connected) {
                server.disconnect();
            }
        }

        public static boolean ping() {
            return LTransport.send_rqst(server, RQST_PING, 0);
        }

        public static boolean requestUserName() {
            return LTransport.send_rqst(server, RQST_CREATE_USER, 0);
        }

        public static boolean getShareUserById(int id) {
            return LTransport.send_rqst(server, RQST_GET_SHARE_USER_BY_ID, id, scrambler);
        }

        public static boolean getShareUserByName(String name) {
            return LTransport.send_rqst(server, RQST_GET_SHARE_USER_BY_NAME, name, scrambler);
        }
    }
}
