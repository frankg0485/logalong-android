package com.swoag.logalong.network;
/* Copyright (C) 2015 - 2017 SWOAG Technology <www.swoag.com> */

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.swoag.logalong.LApp;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LBuffer;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;

import java.util.zip.CRC32;

public class LProtocol {
    private static final String TAG = LProtocol.class.getSimpleName();

    private Object stateLock = new Object();

    private final int STATE_DISCONNECTED = 10;
    private final int STATE_CONNECTED = 30;
    private final int STATE_LOGGED_IN = 40;
    private int state;

    private short serverVersion = 0;
    private CRC32 crc32 = new CRC32();

    //////////////////////////////////////////////////////////////////////
    public static final int PACKET_MAX_PAYLOAD_LEN = 1456;
    public static final int PACKET_MAX_LEN = (PACKET_MAX_PAYLOAD_LEN + 8);

    public static final short PACKET_SIGNATURE1 = (short) 0xffaa;

    private static final short PAYLOAD_DIRECTION_RQST = 0;
    private static final short PAYLOAD_DIRECTION_RSPS = (short) 0x8000;

    private static final short PAYLOAD_TYPE_MASK = 0x1800;
    private static final short PAYLOAD_TYPE_SHIFT = 11;
    private static final short PAYLOAD_VALUE_MASK = 0x07ff;

    public static final short PLAYLOAD_TYPE_SYS = (short) (2 << PAYLOAD_TYPE_SHIFT);
    public static final short PLAYLOAD_TYPE_USER = (short) (3 << PAYLOAD_TYPE_SHIFT);

    public static final short RESPONSE_PARSE_RESULT_DONE = 10;
    public static final short RESPONSE_PARSE_RESULT_MORE2COME = 20;
    public static final short RESPONSE_PARSE_RESULT_ERROR = 99;

    public static final short PUSH_NOTIFICATION = 0x0bad;

    public static int PACKET_PAYLOAD_LENGTH(int payloadLen) {
        return ((((payloadLen) + 3) / 4) * 4);
    }

    private static final short RQST_SYS = PLAYLOAD_TYPE_SYS | PAYLOAD_DIRECTION_RQST;
    private static final short RQST_USER = PLAYLOAD_TYPE_USER | PAYLOAD_DIRECTION_RQST;
    private static final short RSPS = PAYLOAD_DIRECTION_RSPS;

    public static final short RSPS_OK = (short) 0x0010;
    public static final short RSPS_MORE = (short) 0x005a;
    public static final short RSPS_USER_NOT_FOUND = (short) 0xf000;
    public static final short RSPS_WRONG_PASSWORD = (short) 0xf001;
    public static final short RSPS_ACCOUNT_NOT_FOUND = (short) 0xf010;
    public static final short RSPS_ERROR = (short) 0xffff;


    public static final short RQST_SCRAMBLER_SEED = RQST_SYS | 0x100;
    public static final short RQST_GET_USER_BY_NAME = RQST_SYS | 0x200;
    public static final short RQST_CREATE_USER = RQST_SYS | 0x204;
    public static final short RQST_SIGN_IN = RQST_SYS | 0x208;
    public static final short RQST_LOG_IN = RQST_SYS | 0x209;
    public static final short RQST_RESET_PASSWORD = RQST_SYS | 0x20a;
    public static final short RQST_UPDATE_USER_PROFILE = RQST_SYS | 0x20c;

    public static final short JRQST_ADD_ACCOUNT = 0x001;
    public static final short JRQST_UPDATE_ACCOUNT = 0x002;
    public static final short JRQST_DELETE_ACCOUNT = 0x003;

    public static final short JRQST_ADD_CATEGORY = 0x011;
    public static final short JRQST_UPDATE_CATEGORY = 0x012;
    public static final short JRQST_DELETE_CATEGORY = 0x013;

    public static final short JRQST_ADD_TAG = 0x021;
    public static final short JRQST_UPDATE_TAG = 0x022;
    public static final short JRQST_DELETE_TAG = 0x023;

    public static final short JRQST_ADD_VENDOR = 0x031;
    public static final short JRQST_UPDATE_VENDOR = 0x032;
    public static final short JRQST_DELETE_VENDOR = 0x033;

    public static final short JRQST_ADD_RECORD = 0x041;
    public static final short JRQST_UPDATE_RECORD = 0x042;
    public static final short JRQST_DELETE_RECORD = 0x043;

    public static final short JRQST_ADD_SCHEDULE = 0x051;
    public static final short JRQST_UPDATE_SCHEDULE = 0x052;
    public static final short JRQST_DELETE_SCHEDULE = 0x053;

    public static final short JRQST_GET_ACCOUNTS = 0x101;
    public static final short JRQST_GET_CATEGORIES = 0x111;
    public static final short JRQST_GET_TAGS = 0x121;
    public static final short JRQST_GET_VENDORS = 0x131;
    public static final short JRQST_GET_RECORD = 0x141;
    public static final short JRQST_GET_RECORDS = 0x142;
    public static final short JRQST_GET_ACCOUNT_RECORDS = 0x143;
    public static final short JRQST_GET_ACCOUNT_USERS = 0x151;
    public static final short JRQST_GET_SCHEDULE = 0x161;
    public static final short JRQST_GET_SCHEDULES = 0x162;
    public static final short JRQST_GET_ACCOUNT_SCHEDULES = 0x163;

    public static final short JRQST_ADD_USER_TO_ACCOUNT = 0x301;
    public static final short JRQST_REMOVE_USER_FROM_ACCOUNT = 0x302;
    public static final short JRQST_CONFIRM_ACCOUNT_SHARE = 0x303;

    public static final short RQST_POST_JOURNAL = RQST_SYS | 0x555;
    public static final short RQST_POLL = RQST_SYS | 0x777;
    public static final short RQST_POLL_ACK = RQST_SYS | 0x778;
    public static final short RQST_UTC_SYNC = RQST_SYS | 0x7f0;
    public static final short RQST_PING = RQST_SYS | 0x7ff;

    private LBuffer pktBuf;
    private LBuffer pkt;

    private static LProtocol instance;

    public static LProtocol getInstance() {
        if (null == instance) {
            instance = new LProtocol();
        }
        return instance;
    }

    private LProtocol() {
        pktBuf = new LBuffer(PACKET_MAX_LEN * 2);
        state = STATE_DISCONNECTED;
    }

    private void handleSystemMsgBroadcast(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;

        int utc = pkt.getIntAutoInc();
        int bytes = pkt.getShortAutoInc();
        String msg = pkt.getStringAutoInc(bytes);
        String str = msg.replaceAll("\\\\n", "\\\n");
        LPreferences.setServerMsg(str);

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra("cacheId", cacheId);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private class PacketConsumptionStatus {
        int bytesConsumed;
        boolean isResponseCompleted;
    }

    //NOTE: in general, it's a big NO-NO to do anything that's designed for UI thread.
    private PacketConsumptionStatus consumePacket(LBuffer pkt, int scrambler) {
        PacketConsumptionStatus packetConsumptionStatus = new PacketConsumptionStatus();
        packetConsumptionStatus.bytesConsumed = 0;
        packetConsumptionStatus.isResponseCompleted = true;

        //minimum packet length 8 + CRC32
        if (pkt.getLen() < 12) {
            return packetConsumptionStatus;
        }

        Intent rspsIntent;
        int origOffset = pkt.getBufOffset();
        int total = (pkt.getShortAt(origOffset + 2) & 0xfff) + 4; //mask out sequence bits
        short rsps = pkt.getShortAt(origOffset + 4);
        int status;

        //partial packet received, ignore and wait for more data to come
        if (total > pkt.getLen()) return packetConsumptionStatus;

        //verify CRC32
        crc32.reset();
        crc32.update(pkt.getBuf(), pkt.getBufOffset(), total - 4);

        if ((int) crc32.getValue() != pkt.getIntAt(pkt.getBufOffset() + total - 4)) {
            LLog.w(TAG, "drop corrupted packet: checksum mismatch");
            packetConsumptionStatus.bytesConsumed = total; //discard packet
            return packetConsumptionStatus;
        }

        LTransport.scramble(pkt, scrambler);
        pkt.skip(6);
        status = pkt.getShortAutoInc();

        //if ((RSPS | requestCode) != rsps) {
        //    LLog.w(TAG, "protocol failed: unexpected response");
        //    packetConsumptionStatus.bytesConsumed = -1;
        //    return packetConsumptionStatus;
        //}

        //if (status != LProtocol.RSPS_OK && status != LProtocol.RSPS_MORE) {
        //    LLog.w(TAG, "protocol request code: " + requestCode + " error status := " + status);
        //}

        // 'state' is updated only this thread, hence safe to read without lock
        switch (state) {
            case STATE_DISCONNECTED:
                switch (rsps) {
                    case RSPS | RQST_SCRAMBLER_SEED:
                        serverVersion = pkt.getShort();
                        //LLog.d(TAG, "channel scrambler seed sent");
                        synchronized (stateLock) {
                            state = STATE_CONNECTED;
                        }

                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                .ACTION_CONNECTED_TO_SERVER));
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;

                    default:
                        LLog.w(TAG, "unexpected response: " + rsps + "@state: " + state);
                        break;
                }
                break;

            case STATE_CONNECTED:
                switch (rsps) {
                    case RSPS | RQST_GET_USER_BY_NAME: {
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                .ACTION_GET_USER_BY_NAME));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);

                        if (RSPS_OK == status) {
                            String name, fullName;
                            long gid = pkt.getLongAutoInc();
                            int bytes = pkt.getShortAutoInc();
                            name = pkt.getStringAutoInc(bytes);
                            bytes = pkt.getShortAutoInc();
                            fullName = pkt.getStringAutoInc(bytes);

                            rspsIntent.putExtra("id", gid);
                            rspsIntent.putExtra("name", name);
                            rspsIntent.putExtra("fullName", fullName);
                        }
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;
                    }

                    case RSPS | RQST_CREATE_USER:
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                .ACTION_CREATE_USER));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;

                    case RSPS | RQST_SIGN_IN: {
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                .ACTION_SIGN_IN));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                        if (RSPS_OK == status) {
                            LPreferences.setLoginError(false);
                            int bytes = pkt.getShortAutoInc();
                            String name = pkt.getStringAutoInc(bytes);
                            rspsIntent.putExtra("userName", name);
                        }
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;
                    }

                    case RSPS | RQST_LOG_IN:
                        if (RSPS_OK == status) {
                            LPreferences.setLoginError(false);
                            LPreferences.setUserIdNum(pkt.getLongAutoInc());
                            LPreferences.setUserLoginNum(pkt.getLongAutoInc());

                            synchronized (stateLock) {
                                state = STATE_LOGGED_IN;
                            }
                        } else {
                            //login error, remember to force user to login
                            LPreferences.setLoginError(true);
                        }
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                .ACTION_LOG_IN));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;

                    case RSPS | RQST_RESET_PASSWORD:
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_UI_RESET_PASSWORD));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;

                    default:
                        LLog.w(TAG, "unexpected response: " + rsps + "@state: " + state);
                        break;
                }
                break;

            case STATE_LOGGED_IN:
                switch (rsps) {
                    case RSPS | RQST_UPDATE_USER_PROFILE:
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                .ACTION_UPDATE_USER_PROFILE));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;

                    case RSPS | RQST_SIGN_IN: {
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                .ACTION_SIGN_IN));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                        if (RSPS_OK == status) {
                            LPreferences.setLoginError(false);
                            int bytes = pkt.getShortAutoInc();
                            String name = pkt.getStringAutoInc(bytes);
                            rspsIntent.putExtra("userName", name);
                        }
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;
                    }

                    case RSPS | RQST_GET_USER_BY_NAME: {
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                .ACTION_GET_USER_BY_NAME));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);

                        if (RSPS_OK == status) {
                            String name, fullName;
                            long gid = pkt.getLongAutoInc();
                            int bytes = pkt.getShortAutoInc();
                            name = pkt.getStringAutoInc(bytes);
                            bytes = pkt.getShortAutoInc();
                            fullName = pkt.getStringAutoInc(bytes);

                            rspsIntent.putExtra("id", gid);
                            rspsIntent.putExtra("name", name);
                            rspsIntent.putExtra("fullName", fullName);
                            LPreferences.setShareUserId(gid, name);
                            LPreferences.setShareUserName(gid, fullName);
                        }
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;
                    }

                    case RSPS | RQST_RESET_PASSWORD:
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_UI_RESET_PASSWORD));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;

                    case RSPS | RQST_POST_JOURNAL:
                        packetConsumptionStatus.isResponseCompleted = (status != RSPS_MORE);
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_POST_JOURNAL));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                        if (RSPS_OK == status || RSPS_MORE == status) {
                            int journalId = pkt.getIntAutoInc();
                            rspsIntent.putExtra("journalId", journalId);
                            short jrqstId = pkt.getShortAutoInc();
                            rspsIntent.putExtra("jrqstId", jrqstId);
                            short jret = pkt.getShortAutoInc();
                            rspsIntent.putExtra("jret", jret);

                            switch (jrqstId) {
                                case JRQST_ADD_ACCOUNT:
                                    if (RSPS_OK == jret) {
                                        rspsIntent.putExtra("id", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("uid", pkt.getLongAutoInc());
                                    }
                                    break;
                                case JRQST_ADD_CATEGORY:
                                case JRQST_ADD_VENDOR:
                                case JRQST_ADD_TAG:
                                case JRQST_ADD_RECORD:
                                case JRQST_ADD_SCHEDULE:
                                    if (RSPS_OK == jret) {
                                        rspsIntent.putExtra("id", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                    }
                                    break;
                                case JRQST_GET_ACCOUNTS:
                                    if (RSPS_OK == jret) {
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("uid", pkt.getLongAutoInc());
                                        int bytes = pkt.getShortAutoInc();
                                        String name = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("name", name);
                                    }
                                    break;
                                case JRQST_GET_ACCOUNT_USERS:
                                    if (RSPS_OK == jret) {
                                        rspsIntent.putExtra("aid", pkt.getLongAutoInc());
                                        short length = pkt.getShortAutoInc();
                                        String accountUsers = pkt.getStringAutoInc(length);
                                        rspsIntent.putExtra("users", accountUsers);
                                    }
                                    break;
                                case JRQST_GET_CATEGORIES:
                                    if (RSPS_OK == jret) {
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("pgid", pkt.getLongAutoInc());
                                        short bytes = pkt.getShortAutoInc();
                                        String name = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("name", name);
                                    }
                                    break;
                                case JRQST_GET_VENDORS:
                                    if (RSPS_OK == jret) {
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("type", (int) pkt.getByteAutoInc());
                                        short bytes = pkt.getShortAutoInc();
                                        String name = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("name", name);
                                    }
                                    break;
                                case JRQST_GET_TAGS:
                                    if (RSPS_OK == jret) {
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        short bytes = pkt.getShortAutoInc();
                                        String name = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("name", name);
                                    }
                                    break;
                                case JRQST_GET_RECORD:
                                case JRQST_GET_RECORDS:
                                case JRQST_GET_ACCOUNT_RECORDS:
                                    if (RSPS_OK == jret) {
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("aid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("aid2", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("cid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("tid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("vid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("type", pkt.getByteAutoInc());
                                        rspsIntent.putExtra("amount", pkt.getDoubleAutoInc());
                                        rspsIntent.putExtra("createBy", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("changeBy", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("recordId", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("timestamp", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("createTime", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("changeTime", pkt.getLongAutoInc());

                                        short bytes = pkt.getShortAutoInc();
                                        String note = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("note", note);
                                    }
                                    break;

                                case JRQST_GET_SCHEDULE:
                                case JRQST_GET_SCHEDULES:
                                case JRQST_GET_ACCOUNT_SCHEDULES:
                                    if (RSPS_OK == jret) {
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("aid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("aid2", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("cid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("tid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("vid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("type", pkt.getByteAutoInc());
                                        rspsIntent.putExtra("amount", pkt.getDoubleAutoInc());
                                        rspsIntent.putExtra("createBy", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("changeBy", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("recordId", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("timestamp", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("createTime", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("changeTime", pkt.getLongAutoInc());

                                        short bytes = pkt.getShortAutoInc();
                                        String note = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("note", note);

                                        rspsIntent.putExtra("nextTime", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("interval", pkt.getByteAutoInc());
                                        rspsIntent.putExtra("unit", pkt.getByteAutoInc());
                                        rspsIntent.putExtra("count", pkt.getByteAutoInc());
                                        rspsIntent.putExtra("enabled", pkt.getByteAutoInc());
                                    }
                                    break;
                            }
                        }

                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;

                    case RSPS | RQST_POLL:
                        packetConsumptionStatus.isResponseCompleted = (status == RSPS_OK || status == RSPS_ERROR);
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_POLL));
                        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                        if (status == RSPS_OK) {
                            rspsIntent.putExtra("id", pkt.getLongAutoInc());
                            rspsIntent.putExtra("nid", pkt.getShortAutoInc());
                            rspsIntent.putExtra("int1", pkt.getLongAutoInc());
                            rspsIntent.putExtra("int2", pkt.getLongAutoInc());
                            int bytes = pkt.getShortAutoInc();
                            String txt = pkt.getStringAutoInc(bytes);
                            rspsIntent.putExtra("txt1", txt);
                            bytes = pkt.getShortAutoInc();
                            txt = pkt.getStringAutoInc(bytes);
                            rspsIntent.putExtra("txt2", txt);

                            bytes = pkt.getShortAutoInc();
                            rspsIntent.putExtra("blob", pkt.getBytesAutoInc(bytes));
                        }
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;

                    case RSPS | RQST_POLL_ACK:
                        if (status == RSPS_OK) {
                            rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_POLL_ACK));
                            LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        } else {
                            LLog.w(TAG, "unable to acknowledge polling");
                        }
                        break;

                    case PUSH_NOTIFICATION:
                        LLog.d(TAG, "push notify received");
                        rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_PUSH_NOTIFICATION));
                        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        break;

                    default:
                        LLog.w(TAG, "unexpected response: " + rsps + "@state: " + state);
                        break;
                }
                break;
        }
        pkt.setBufOffset(origOffset);

        packetConsumptionStatus.bytesConsumed = total;
        return packetConsumptionStatus;
    }

    private boolean alignPacket(LBuffer pkt) {
        if (pkt.getShort() == PACKET_SIGNATURE1) return true;
        LLog.w(TAG, "packet misaligned");

        while (pkt.getLen() >= 12) { //minimum packet length 8 + CRC32
            if (pkt.getShort() == PACKET_SIGNATURE1) return true;
            pkt.skip(1);
            pkt.modLen(-1);
        }
        return false;
    }

    public void shutdown() {
        LLog.d(TAG, "network thread is shutting down, socket is no longer connected");
        synchronized (stateLock) {
            state = STATE_DISCONNECTED;
        }
    }

    // parser runs in Network receiving thread, thus no GUI update here.
    public short parse(LBuffer buf, int scrambler) {
        if (pktBuf.getLen() > 0) {
            //LLog.d(TAG, "packet pipe fragmented");
            pktBuf.reset();
            pktBuf.append(buf);
            pkt = pktBuf;
        } else {
            pkt = buf;
        }

        while (alignPacket(pkt)) {
            PacketConsumptionStatus status = consumePacket(pkt, scrambler);
            int bytes = status.bytesConsumed;
            if (bytes == -1) {
                LLog.e(TAG, "packet parse error?");
                break;
            } else if (bytes == 0) {
                //packet not consumed
                if (pkt != pktBuf) {
                    //LLog.d(TAG, "reset packet then append");
                    pkt.reset();
                    pktBuf.reset();
                    pktBuf.append(pkt);
                } else {
                    //this must be the case where the data is only partially received.
                    //reset packet then quit the loop
                    //LLog.d(TAG, "reset packet");
                    pkt.reset();
                }
                return RESPONSE_PARSE_RESULT_MORE2COME;
            } else {
                //packet consumed
                pkt.setLen(pkt.getLen() - bytes);
                pkt.setBufOffset((pkt.getLen() == 0) ? 0 : pkt.getBufOffset() + bytes);

                //LLog.d(TAG, "continue parsing: " + buf.getLen() + " offset: " + pkt
                // .getBufOffset());
                //LLog.d(TAG, LLog.bytesToHex(pkt.getBuf()));
                if (status.isResponseCompleted) return RESPONSE_PARSE_RESULT_DONE;
            }
        }
        //unexpected: unable to align packet
        return RESPONSE_PARSE_RESULT_ERROR;
    }

    // all user interface calls
    public boolean isConnected() {
        synchronized (stateLock) {
            return state >= STATE_CONNECTED;
        }
    }

    public boolean isLoggedIn() {
        synchronized (stateLock) {
            return state >= STATE_LOGGED_IN;
        }
    }

    public short getServerVersion() {
        synchronized (stateLock) {
            return state >= STATE_CONNECTED ? serverVersion : 0;
        }
    }
}
