package com.swoag.logalong.network;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.swoag.logalong.LApp;
import com.swoag.logalong.entities.LAccountShareRequest;
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

    public static final short RSPS_OK = (short) 0x0010;
    public static final short RSPS_ERROR = (short) 0xffff;

    private static final short RQST_SCRAMBLER_SEED = RQST_SYS | 0x100;
    private static final short RQST_CREATE_USER = RQST_SYS | 0x104;
    private static final short RQST_LOGIN = RQST_SYS | 0x105;
    private static final short RQST_UPDATE_USER_PROFILE = RQST_SYS | 0x106;
    private static final short RQST_GET_SHARE_USER_BY_ID = RQST_SYS | 0x108;
    private static final short RQST_GET_SHARE_USER_BY_NAME = RQST_SYS | 0x109;
    private static final short RQST_SHARE_ACCOUNT_WITH_USER = RQST_SYS | 0x10c;
    private static final short RQST_CONFIRM_ACCOUNT_SHARE = RQST_SYS | 0x10d;
    private static final short RQST_SHARE_TRANSITION_RECORD = RQST_SYS | 0x110;
    private static final short RQST_SHARE_ACCOUNT_USER_CHANGE = RQST_SYS | 0x114;
    private static final short RQST_POST_JOURNAL = RQST_SYS | 0x555;
    private static final short RQST_POLL = RQST_SYS | 0x777;
    private static final short RQST_POLL_ACK = RQST_SYS | 0x778;
    private static final short RQST_PING = RQST_SYS | 0x7ff;

    private static final short CMD_SHARE_ACCOUNT_REQUEST = 0x0004;
    private static final short CMD_CONFIRMED_ACCOUNT_SHARE = 0x0008;
    private static final short CMD_SHARED_TRANSITION_RECORD = 0x000c;
    private static final short CMD_RECEIVED_JOURNAL = 0x0010;
    private static final short CMD_SHARE_ACCOUNT_USER_CHANGE = 0x0014;

    private LBuffer pktBuf;
    private LBuffer pkt;
    private short[] shorts;

    public LProtocol() {
        pktBuf = new LBuffer(PACKET_MAX_LEN * 2);
        shorts = new short[PACKET_MAX_LEN];
    }

    private void handleJournalReceive(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int userId = pkt.getIntAutoInc();
        short bytes = pkt.getShortAutoInc();
        String userName = pkt.getStringAutoInc(bytes);
        bytes = pkt.getShortAutoInc();
        String userFullName = pkt.getStringAutoInc(bytes);

        bytes = pkt.getShortAutoInc();
        String record = pkt.getStringAutoInc(bytes);

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("id", userId);
        rspsIntent.putExtra("userName", userName);
        rspsIntent.putExtra("userFullName", userFullName);
        rspsIntent.putExtra("record", record);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private void handleAccountShareRequest(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int userId = pkt.getIntAutoInc();
        short bytes = pkt.getShortAutoInc();
        String userName = pkt.getStringAutoInc(bytes);
        bytes = pkt.getShortAutoInc();
        String userFullName = pkt.getStringAutoInc(bytes);

        bytes = pkt.getShortAutoInc();
        String str = pkt.getStringAutoInc(bytes);
        String[] ss = str.split(",");
        String accountName = ss[0];
        String uuid = ss[1];
        byte requireConfirmation = Byte.parseByte(ss[2]);
        LLog.d(TAG, "account share request from: " + userId + ":" + userName + " account: " + accountName + " " + uuid);

        if (requireConfirmation == 1) {
            LPreferences.addAccountShareRequest(new LAccountShareRequest(userId, userName, userFullName, accountName, uuid));
        }

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("id", userId);
        rspsIntent.putExtra("userName", userName);
        rspsIntent.putExtra("userFullName", userFullName);
        rspsIntent.putExtra("accountName", accountName);
        rspsIntent.putExtra("UUID", uuid);
        rspsIntent.putExtra("requireConfirmation", requireConfirmation);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private void handleAccountShareConfirm(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int userId = pkt.getIntAutoInc();
        short bytes = pkt.getShortAutoInc();
        String userName = pkt.getStringAutoInc(bytes);
        bytes = pkt.getShortAutoInc();
        String userFullName = pkt.getStringAutoInc(bytes);

        bytes = pkt.getShortAutoInc();
        String accountName = pkt.getStringAutoInc(bytes);
        LLog.d(TAG, "account share request from: " + userId + ":" + userName + " account: " + accountName);

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("id", userId);
        rspsIntent.putExtra("userName", userName);
        rspsIntent.putExtra("userFullName", userFullName);
        rspsIntent.putExtra("accountName", accountName);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private void handleRecordShare(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int userId = pkt.getIntAutoInc();
        short bytes = pkt.getShortAutoInc();
        String userName = pkt.getStringAutoInc(bytes);
        bytes = pkt.getShortAutoInc();
        String userFullName = pkt.getStringAutoInc(bytes);

        bytes = pkt.getShortAutoInc();
        String record = pkt.getStringAutoInc(bytes);
        LLog.d(TAG, "record share request from: " + userId + ":" + userName + " record: " + record);

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("id", userId);
        rspsIntent.putExtra("userName", userName);
        rspsIntent.putExtra("userFullName", userFullName);
        rspsIntent.putExtra("record", record);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private void handleShareAccountUserChange(LBuffer pkt, int status, int action, int cacheId) {
        Intent rspsIntent;
        int userId = pkt.getIntAutoInc();
        short bytes = pkt.getShortAutoInc();
        String userName = pkt.getStringAutoInc(bytes);
        bytes = pkt.getShortAutoInc();
        String userFullName = pkt.getStringAutoInc(bytes);

        bytes = pkt.getShortAutoInc();
        String str = pkt.getStringAutoInc(bytes);
        String[] ss = str.split(",");
        int changeUserId = Integer.parseInt(ss[0]);
        byte change = Byte.parseByte(ss[1]);
        String accountName = ss[2];
        String uuid = ss[3];
        LLog.d(TAG, "account share change request from: " + userId + ":" + userName + " account: " + accountName + " " + uuid);

        rspsIntent = new Intent(LBroadcastReceiver.action(action));
        rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
        rspsIntent.putExtra("cacheId", cacheId);
        rspsIntent.putExtra("id", userId);
        rspsIntent.putExtra("userName", userName);
        rspsIntent.putExtra("userFullName", userFullName);
        rspsIntent.putExtra("changeUserId", changeUserId);
        rspsIntent.putExtra("change", change);
        rspsIntent.putExtra("accountName", accountName);
        rspsIntent.putExtra("UUID", uuid);
        LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
    }

    private int consumePacket(LBuffer pkt) {
        Intent rspsIntent;
        int origOffset = pkt.getBufOffset();
        short total = pkt.getShortAt(origOffset + 2);
        short rsps = pkt.getShortAt(origOffset + 4);
        int status;

        LTransport.scramble(pkt, scrambler);
        pkt.skip(6);
        status = pkt.getShortAutoInc();

        switch (rsps) {
            case RSPS | RQST_PING:
                //LLog.d(TAG, "pong");
                break;

            case RSPS | RQST_SCRAMBLER_SEED:
                //LLog.d(TAG, "channel scrambler seed sent");
                connected = true;
                break;

            case RSPS | RQST_CREATE_USER:
                if (status == RSPS_OK) {
                    int userId = pkt.getIntAutoInc();
                    short bytes = pkt.getShortAutoInc();
                    String userName = pkt.getStringAutoInc(bytes);

                    rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_USER_CREATED));
                    rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                    rspsIntent.putExtra("id", userId);
                    rspsIntent.putExtra("name", userName);
                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                } else {
                    LLog.w(TAG, "unable to create user");
                }
                break;

            case RSPS | RQST_LOGIN:
                rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_LOGIN));
                rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                break;

            case RSPS | RQST_UPDATE_USER_PROFILE:
                rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_USER_PROFILE_UPDATED));
                rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                break;

            case RSPS | RQST_GET_SHARE_USER_BY_ID:
                rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_GET_SHARE_USER_BY_ID));
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
                rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_GET_SHARE_USER_BY_NAME));
                rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                if (status == RSPS_OK) {
                    int userId = pkt.getIntAutoInc();
                    short bytes = pkt.getShortAutoInc();
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

            case RSPS | RQST_SHARE_ACCOUNT_WITH_USER:
                rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_SHARE_ACCOUNT_WITH_USER));
                rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                if (status == RSPS_OK) {
                    int userId = pkt.getIntAutoInc();
                    short bytes = pkt.getShortAutoInc();
                    String str = pkt.getStringAutoInc(bytes);
                    String[] ss = str.split(",");
                    rspsIntent.putExtra("id", userId);
                    rspsIntent.putExtra("accountName", ss[0]);
                    rspsIntent.putExtra("UUID", ss[1]);
                    rspsIntent.putExtra("requireConfirmation", Byte.parseByte(ss[2]));
                }
                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                break;

            case RSPS | RQST_CONFIRM_ACCOUNT_SHARE:
                break;

            case RSPS | RQST_SHARE_TRANSITION_RECORD:
                break;

            case RSPS | RQST_SHARE_ACCOUNT_USER_CHANGE:
                break;

            case RSPS | RQST_POST_JOURNAL:
                rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_JOURNAL_POSTED));
                rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, status);
                if (status == RSPS_OK) {
                    int userId = pkt.getIntAutoInc();
                    short bytes = pkt.getShortAutoInc();
                    String str = pkt.getStringAutoInc(bytes);
                    String[] ss = str.split(":");
                    long journalId = Long.parseLong(ss[0]);
                    rspsIntent.putExtra("journalId", journalId);
                }
                LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                break;

            case RSPS | RQST_POLL:
                if (status == RSPS_OK) {
                    int cacheId = pkt.getIntAutoInc();
                    short cmd = pkt.getShortAutoInc();
                    switch (cmd) {
                        case CMD_SHARE_ACCOUNT_REQUEST:
                            handleAccountShareRequest(pkt, status, LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH, cacheId);
                            break;

                        case CMD_CONFIRMED_ACCOUNT_SHARE:
                            handleAccountShareConfirm(pkt, status, LBroadcastReceiver.ACTION_CONFIRMED_ACCOUNT_SHARE, cacheId);
                            break;

                        case CMD_SHARED_TRANSITION_RECORD:
                            handleRecordShare(pkt, status, LBroadcastReceiver.ACTION_SHARED_TRANSITION_RECORD, cacheId);
                            break;

                        case CMD_RECEIVED_JOURNAL:
                            handleJournalReceive(pkt, status, LBroadcastReceiver.ACTION_JOURNAL_RECEIVED, cacheId);
                            break;

                        case CMD_SHARE_ACCOUNT_USER_CHANGE:
                            handleShareAccountUserChange(pkt, status, LBroadcastReceiver.ACTION_SHARE_ACCOUNT_USER_CHANGE, cacheId);
                            break;
                    }
                } else {
                    rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_POLL_IDLE));
                    rspsIntent.putExtra(LBroadcastReceiver.EXTRA_RET_CODE, (int)RSPS_OK);
                    LocalBroadcastManager.getInstance(LApp.ctx).sendBroadcast(rspsIntent);
                }
                break;

            case RSPS | RQST_POLL_ACK:
                rspsIntent = new Intent(LBroadcastReceiver.action(LBroadcastReceiver.ACTION_POLL_ACKED));
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
            }
        }

        public static void disconnect() {
            server.disconnect();
            //disconnect eventually clears the 'connected' flag thru callback, when
            //network thread exits
        }

        public static boolean isConnected() {
            return connected;
        }

        public static void initScrambler() {
            scrambler = genScrambler();
            LTransport.send_rqst(server, RQST_SCRAMBLER_SEED, scrambler, 0);
        }

        public static boolean ping() {
            return LTransport.send_rqst(server, RQST_PING, 0);
        }

        public static boolean requestUserName() {
            return LTransport.send_rqst(server, RQST_CREATE_USER, 0);
        }

        public static boolean login() {
            return LTransport.send_rqst(server, RQST_LOGIN, LPreferences.getUserId(), LPreferences.getUserName(), scrambler);
        }

        public static boolean updateUserProfile() {
            return LTransport.send_rqst(server, RQST_UPDATE_USER_PROFILE, LPreferences.getUserId(), LPreferences.getUserFullName(), scrambler);
        }

        public static boolean getShareUserById(int id) {
            return LTransport.send_rqst(server, RQST_GET_SHARE_USER_BY_ID, id, scrambler);
        }

        public static boolean getShareUserByName(String name) {
            return LTransport.send_rqst(server, RQST_GET_SHARE_USER_BY_NAME, name, scrambler);
        }

        public static boolean shareAccountWithUser(int userId, String accountName, String uuid, boolean requireConfirmation) {
            return LTransport.send_rqst(server, RQST_SHARE_ACCOUNT_WITH_USER, userId, accountName + "," + uuid + ","
                    + (requireConfirmation ? 1 : 0), scrambler);
        }

        public static boolean shareTransitionRecord(int userId, String record) {
            return LTransport.send_rqst(server, RQST_SHARE_TRANSITION_RECORD, userId, record, scrambler);
        }

        public static boolean poll() {
            return LTransport.send_rqst(server, RQST_POLL, scrambler);
        }

        public static boolean pollAck(int cacheId) {
            return LTransport.send_rqst(server, RQST_POLL_ACK, cacheId, scrambler);
        }

        public static boolean confirmAccountShare(int userId, String accountName) {
            return LTransport.send_rqst(server, RQST_CONFIRM_ACCOUNT_SHARE, userId, accountName, scrambler);
        }

        public static boolean postJournal(int userId, String record) {
            return LTransport.send_rqst(server, RQST_POST_JOURNAL, userId, record, scrambler);
        }

        public static boolean shareAccountUserChange(int userId, int changeUserId, boolean add, String accountName, String uuid) {
            if ((userId == changeUserId) && add) return false;
            return LTransport.send_rqst(server, RQST_SHARE_ACCOUNT_USER_CHANGE, userId, changeUserId + ","
                    + (add ? 1 : 0) + ',' + accountName + "," + uuid, scrambler);
        }
    }
}
