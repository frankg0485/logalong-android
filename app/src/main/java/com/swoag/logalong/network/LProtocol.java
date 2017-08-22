package com.swoag.logalong.network;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccountShareRequest;
import com.swoag.logalong.entities.LJournal;
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

    public static int PACKET_PAYLOAD_LENGTH(int payloadLen) {
        return ((((payloadLen) + 3) / 4) * 4);
    }

    private static final short RQST_SYS = PLAYLOAD_TYPE_SYS | PAYLOAD_DIRECTION_RQST;
    private static final short RQST_USER = PLAYLOAD_TYPE_USER | PAYLOAD_DIRECTION_RQST;
    private static final short RSPS = PAYLOAD_DIRECTION_RSPS;

    public static final short RSPS_OK = (short) 0x0010;
    public static final short RSPS_MORE = (short) 0x005a;
    public static final short RSPS_USER_NOT_FOUND = (short) 0xf000;
    public static final short RSPS_WRONG_PASSWORD = (short) 0xf0001;
    public static final short RSPS_ACCOUNT_NOT_FOUND = (short) 0xf010;
    public static final short RSPS_ERROR = (short) 0xffff;

    public static final short RQST_SCRAMBLER_SEED = RQST_SYS | 0x100;
    public static final short RQST_GET_USER_BY_NAME = RQST_SYS | 0x200;
    public static final short RQST_CREATE_USER = RQST_SYS | 0x204;
    public static final short RQST_SIGN_IN = RQST_SYS | 0x208;
    public static final short RQST_LOG_IN = RQST_SYS | 0x209;
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

    public static final short JRQST_ADD_RECORD = 0x005;
    public static final short JRQST_GET_ACCOUNTS = 0x101;
    public static final short JRQST_GET_CATEGORIES = 0x102;
    public static final short JRQST_GET_TAGS = 0x103;
    public static final short JRQST_GET_VENDORS = 0x104;
    public static final short JRQST_GET_RECORDS = 0x105;

    public static final short RQST_GET_SHARE_USER_BY_NAME = RQST_SYS | 0x109;
    public static final short RQST_POST_JOURNAL = RQST_SYS | 0x555;
    public static final short RQST_POLL = RQST_SYS | 0x777;
    public static final short RQST_POLL_ACK = RQST_SYS | 0x778;
    public static final short RQST_UTC_SYNC = RQST_SYS | 0x7f0;
    public static final short RQST_PING = RQST_SYS | 0x7ff;


    private static final short CMD_JRQST_MASK = (short) 0x8000;
    private static final short CMD_SHARE_TRANSITION_RECORD = (short) (CMD_JRQST_MASK | LJournal
            .JRQST_SHARE_TRANSITION_RECORD);
    private static final short CMD_SHARE_TRANSITION_RECORDS = (short) (CMD_JRQST_MASK | LJournal
            .JRQST_SHARE_TRANSITION_RECORDS);
    private static final short CMD_SHARE_TRANSITION_CATEGORY = (short) (CMD_JRQST_MASK | LJournal
            .JRQST_SHARE_TRANSITION_CATEGORY);
    private static final short CMD_SHARE_TRANSITION_PAYER = (short) (CMD_JRQST_MASK | LJournal
            .JRQST_SHARE_TRANSITION_PAYER);
    private static final short CMD_SHARE_TRANSITION_TAG = (short) (CMD_JRQST_MASK | LJournal
            .JRQST_SHARE_TRANSITION_TAG);
    private static final short CMD_SHARE_PAYER_CATEGORY = (short) (CMD_JRQST_MASK | LJournal
            .JRQST_SHARE_PAYER_CATEGORY);
    private static final short CMD_SHARE_SCHEDULE = (short) (CMD_JRQST_MASK | LJournal
            .JRQST_SHARE_SCHEDULE);

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

    private void handleAccountShareRequest(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int userId = pkt.getIntAutoInc();
        byte bytes = pkt.getByteAutoInc();
        String userName = pkt.getStringAutoInc(bytes);
        bytes = pkt.getByteAutoInc();
        String userFullName = pkt.getStringAutoInc(bytes);
        int accountGid = pkt.getIntAutoInc();
        int shareAccountGid = pkt.getIntAutoInc();
        bytes = pkt.getByteAutoInc();
        String accountName = pkt.getStringAutoInc(bytes);

        LLog.d(TAG, "account share request from: " + userId + ":" + userName + " account: " +
                accountName + " GID: " + accountGid);
        LPreferences.addAccountShareRequest(new LAccountShareRequest(userId, userName,
                userFullName, accountName, accountGid, shareAccountGid));

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private void handleAccountShareUpdate(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int accountGid = pkt.getIntAutoInc();
        int numShareUsers = pkt.getShortAutoInc();
        int[] shareUsers = new int[numShareUsers];
        for (int ii = 0; ii < numShareUsers; ii++)
            shareUsers[ii] = pkt.getIntAutoInc();

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("accountGid", accountGid);
        rspsIntent.putExtra("numShareUsers", (short) numShareUsers);
        rspsIntent.putExtra("shareUsers", shareUsers);
        LLog.d(TAG, "update account " + accountGid + " share num: " + numShareUsers + " : " +
                shareUsers);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

     private void handleShareUserProfileUpdate(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int userId = pkt.getIntAutoInc();
        byte bytes = pkt.getByteAutoInc();
        String userName = pkt.getStringAutoInc(bytes);
        bytes = pkt.getByteAutoInc();
        String userFullName = pkt.getStringAutoInc(bytes);

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("userId", userId);
        rspsIntent.putExtra("userName", userName);
        rspsIntent.putExtra("userFullName", userFullName);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private void handleRecordShare(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int accountGid = pkt.getIntAutoInc();
        int bytes = pkt.getShortAutoInc();
        String record = pkt.getStringAutoInc(bytes);
        //LLog.d(TAG, "record share request for account: " + accountGid + " record: " + record);

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("accountGid", accountGid);
        rspsIntent.putExtra("record", record);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private void handleScheduleShare(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int accountGid = pkt.getIntAutoInc();
        int accountGid2 = pkt.getIntAutoInc();
        int bytes = pkt.getShortAutoInc();
        String schedule = pkt.getStringAutoInc(bytes);
        //LLog.d(TAG, "schedule share request for account: " + accountGid + " schedule: " +
        // schedule);

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("accountGid", accountGid);
        rspsIntent.putExtra("accountGid2", accountGid2);
        rspsIntent.putExtra("schedule", schedule);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private static final int RECORDS_SHARE_STATE_IDLE = 10;
    private static final int RECORDS_SHARE_STATE_RECEIVING = 20;
    private int recordsShareState = RECORDS_SHARE_STATE_IDLE;
    private int expectedRecordNum;
    private int expectedAccountGid;
    private int expectedCacheId;
    private int receivedRecordNum;
    public static final int RECORDS_RECEIVED_FULL = 10;
    public static final int RECORDS_RECEIVED_PART = 20;
    public static final int RECORDS_RECEIVING = 30;

    private void handleRecordsShare(LBuffer pkt, int status, int action, int cacheId, boolean
            lastPacket) {
        Intent rspsIntent;
        int accountGid = pkt.getIntAutoInc();
        int num = pkt.getShortAutoInc();
        int bytes = pkt.getShortAutoInc();
        String record = pkt.getStringAutoInc(bytes);
        //LLog.d(TAG, "records share request for account: " + accountGid + " record: " + record);

        switch (recordsShareState) {
            case RECORDS_SHARE_STATE_IDLE:
                expectedCacheId = cacheId;
                expectedAccountGid = accountGid;
                expectedRecordNum = num;
                receivedRecordNum = 1;
                //LLog.d(TAG, "receiving cache: " + cacheId + " num: " + num);
                recordsShareState = RECORDS_SHARE_STATE_RECEIVING;
                break;

            case RECORDS_SHARE_STATE_RECEIVING:
                if (expectedAccountGid != accountGid || expectedRecordNum != num || cacheId !=
                        expectedCacheId) {
                    LLog.w(TAG, "share records request out of sync, reset");
                    expectedAccountGid = accountGid;
                    expectedRecordNum = num;
                    receivedRecordNum = 1;
                } else {
                    receivedRecordNum++;
                    //LLog.d(TAG, "received cache: " + cacheId + " num: " + receivedRecordNum + "
                    // of: " + expectedRecordNum);
                }
                break;
        }

        boolean done = receivedRecordNum == expectedRecordNum;
        if (done || lastPacket) recordsShareState = RECORDS_SHARE_STATE_IDLE;

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("accountGid", accountGid);
        rspsIntent.putExtra("record", record);
        if (lastPacket) {
            rspsIntent.putExtra("done", done ? RECORDS_RECEIVED_FULL : RECORDS_RECEIVED_PART);
        } else {
            if (done) {
                LLog.e(TAG, "unexpected: records broken");
            }
            rspsIntent.putExtra("done", RECORDS_RECEIVING);
        }
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private void handleCategoryPayerTagShare(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int bytes = pkt.getShortAutoInc();
        String record = pkt.getStringAutoInc(bytes);

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("record", record);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
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

    private void handlerUnknownMsg(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra("cacheId", cacheId);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private class PacketConsumptionStatus {
        int bytesConsumed;
        boolean isResponseCompleted;
    }

    ;

    //NOTE: in general, it's a big NO-NO to do anything that's designed for UI thread.
    private PacketConsumptionStatus consumePacket(LBuffer pkt, short requestCode, int scrambler) {
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

        if ((RSPS | requestCode) != rsps) {
            LLog.w(TAG, "protocol failed: unexpected response");
            packetConsumptionStatus.bytesConsumed = -1;
            return packetConsumptionStatus;
        }

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
                            int bytes = pkt.getShortAutoInc();
                            name = pkt.getStringAutoInc(bytes);
                            bytes = pkt.getShortAutoInc();
                            fullName = pkt.getStringAutoInc(bytes);

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

                            rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                                    .ACTION_LOG_IN));
                            rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                            LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                        } else {
                            //login error, remember to force user to login
                            LPreferences.setLoginError(true);
                        }
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
                            if (RSPS_OK == jret) {
                                switch (jrqstId) {
                                    case JRQST_ADD_ACCOUNT:
                                        rspsIntent.putExtra("id", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("uid", pkt.getLongAutoInc());
                                        break;
                                    case JRQST_ADD_CATEGORY:
                                    case JRQST_ADD_VENDOR:
                                    case JRQST_ADD_TAG:
                                    case JRQST_ADD_RECORD:
                                        rspsIntent.putExtra("id", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        break;
                                    case JRQST_GET_ACCOUNTS:
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("uid", pkt.getLongAutoInc());
                                        int bytes = pkt.getShortAutoInc();
                                        String name = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("name", name);
                                        break;
                                    case JRQST_GET_CATEGORIES:
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("pgid", pkt.getLongAutoInc());
                                        bytes = pkt.getShortAutoInc();
                                        name = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("name", name);
                                        break;
                                    case JRQST_GET_VENDORS:
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("type", (int) pkt.getByteAutoInc());
                                        bytes = pkt.getShortAutoInc();
                                        name = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("name", name);
                                        break;
                                    case JRQST_GET_TAGS:
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        bytes = pkt.getShortAutoInc();
                                        name = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("name", name);
                                        break;
                                    case JRQST_GET_RECORDS:
                                        rspsIntent.putExtra("gid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("aid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("aid2", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("cid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("tid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("vid", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("type", pkt.getByteAutoInc());
                                        rspsIntent.putExtra("amount", pkt.getDoubleAutoInc());
                                        rspsIntent.putExtra("createBy", pkt.getIntAutoInc());
                                        rspsIntent.putExtra("changeBy", pkt.getIntAutoInc());
                                        rspsIntent.putExtra("timestamp", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("createTime", pkt.getLongAutoInc());
                                        rspsIntent.putExtra("changeTime", pkt.getLongAutoInc());

                                        bytes = pkt.getShortAutoInc();
                                        String note = pkt.getStringAutoInc(bytes);
                                        rspsIntent.putExtra("note", note);
                                        break;
                                }
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
                            rspsIntent.putExtra("int3", pkt.getIntAutoInc());
                            rspsIntent.putExtra("int4", pkt.getIntAutoInc());
                            int bytes = pkt.getShortAutoInc();
                            String txt = pkt.getStringAutoInc(bytes);
                            rspsIntent.putExtra("txt1", txt);
                            bytes = pkt.getShortAutoInc();
                            txt = pkt.getStringAutoInc(bytes);
                            rspsIntent.putExtra("txt2", txt);
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

                    default:
                        LLog.w(TAG, "unexpected response: " + rsps + "@state: " + state);
                        break;
                }
                break;
        }

        /*
        switch (rsps) {
            case RSPS | RQST_PING:
                //LLog.d(TAG, "pong");
                break;

            case RSPS | RQST_GET_SHARE_USER_BY_NAME:
                rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                .ACTION_GET_SHARE_USER_BY_NAME));
                rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                if (status == RSPS_OK) {
                    int userId = pkt.getIntAutoInc();
                    int bytes = pkt.getShortAutoInc();
                    String userName = pkt.getStringAutoInc(bytes);
                    bytes = pkt.getShortAutoInc();
                    String userFullName = pkt.getStringAutoInc(bytes);
                    rspsIntent.putExtra("id", userId);
                    rspsIntent.putExtra("name", userName);
                    rspsIntent.putExtra("fullName", userFullName);
                    LLog.d(TAG, "user returned as: " + userName + " full name: " + userFullName);
                }
                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                break;

            case RSPS | RQST_POLL:
                packetConsumptionStatus.isResponseCompleted = (status == RSPS_OK || status ==
                RSPS_ERROR);
                if (status == RSPS_OK || status == RSPS_ACK) {
                    int cacheId = pkt.getIntAutoInc();
                    short cmd = pkt.getShortAutoInc();
                    switch (cmd) {
                        case CMD_SET_ACCOUNT_GID:
                            handleAccountSetGid(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_SET_ACCOUNT_GID, cacheId);
                            break;
                        case CMD_REQUEST_ACCOUNT_SHARE:
                            handleAccountShareRequest(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH, cacheId);
                            break;
                        case CMD_UPDATE_ACCOUNT_SHARE:
                            handleAccountShareUpdate(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_UPDATE_ACCOUNT_SHARE, cacheId);
                            break;
                        case CMD_UPDATE_ACCOUNT_INFO:
                            handleAccountInfoUpdate(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_UPDATE_ACCOUNT_INFO, cacheId);
                            break;
                        case CMD_UPDATE_SHARE_USER_PROFILE:
                            handleShareUserProfileUpdate(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_UPDATE_SHARE_USER_PROFILE, cacheId);
                            break;

                        case CMD_SHARE_TRANSITION_RECORD:
                            handleRecordShare(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORD, cacheId);
                            break;

                        case CMD_SHARE_TRANSITION_RECORDS:
                            handleRecordsShare(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORDS, cacheId, status ==
                            RSPS_OK);
                            break;

                        case CMD_SHARE_TRANSITION_CATEGORY:
                            handleCategoryPayerTagShare(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_SHARE_TRANSITION_CATEGORY, cacheId);
                            break;

                        case CMD_SHARE_TRANSITION_PAYER:
                            handleCategoryPayerTagShare(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_SHARE_TRANSITION_PAYER, cacheId);
                            break;

                        case CMD_SHARE_TRANSITION_TAG:
                            handleCategoryPayerTagShare(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_SHARE_TRANSITION_TAG, cacheId);
                            break;

                        case CMD_SHARE_PAYER_CATEGORY:
                            handleCategoryPayerTagShare(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_SHARE_PAYER_CATEGORY, cacheId);
                            break;

                        case CMD_SHARE_SCHEDULE:
                            handleScheduleShare(pkt, status, LBroadcastReceiver
                            .ACTION_REQUESTED_TO_SHARE_SCHEDULE, cacheId);
                            break;

                        case CMD_SYSTEM_MSG_BROADCAST:
                            handleSystemMsgBroadcast(pkt, status, LBroadcastReceiver
                            .ACTION_SERVER_BROADCAST_MSG_RECEIVED, cacheId);
                            break;

                        default:
                            LLog.w(TAG, "ignore: unknown command: " + cmd + " cacheId: " + cacheId);
                            handlerUnknownMsg(pkt, status, LBroadcastReceiver.ACTION_UNKNOWN_MSG,
                             cacheId);
                            break;
                    }
                } else {
                    rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver
                    .ACTION_POLL_IDLE));
                    rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, (int) RSPS_OK);
                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                }
                break;


            case RSPS | RQST_UTC_SYNC:
                if (status == RSPS_OK) {
                    long myUtc = pkt.getLongAutoInc();
                    long serverUtc = pkt.getLongAutoInc();
                    long now = System.currentTimeMillis() / 1000;
                    if (now < myUtc) {
                        LLog.w(TAG, "ignored invalid UTC: " + myUtc);
                    } else {
                        long delta = serverUtc + (now - myUtc) / 2 - now;
                        //LLog.d(TAG, "utc delta: " + delta + " " + serverUtc + " " + myUtc + " "
                         +  now);
                        LPreferences.setUtcDelta(delta);
                    }
                } else {
                    LLog.w(TAG, "unable to sync UTC");
                }
                break;
        }*/

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
    public short parse(LBuffer buf, short requestCode, int scrambler) {
        if (pktBuf.getLen() > 0) {
            //LLog.d(TAG, "packet pipe fragmented");
            pktBuf.reset();
            pktBuf.append(buf);
            pkt = pktBuf;
        } else {
            pkt = buf;
        }

        while (alignPacket(pkt)) {
            PacketConsumptionStatus status = consumePacket(pkt, requestCode, scrambler);
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
