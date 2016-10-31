package com.swoag.logalong;
/* Copyright (C) 2015 - 2016 SWOAG Technology <www.swoag.com> */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import com.swoag.logalong.entities.LAccount;
import com.swoag.logalong.entities.LJournal;
import com.swoag.logalong.network.LAppServer;
import com.swoag.logalong.network.LProtocol;
import com.swoag.logalong.utils.DBAccount;
import com.swoag.logalong.utils.DBScheduledTransaction;
import com.swoag.logalong.utils.LBroadcastReceiver;
import com.swoag.logalong.utils.LLog;
import com.swoag.logalong.utils.LPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class MainService extends Service implements LBroadcastReceiver.BroadcastReceiverListener {
    private static final String TAG = MainService.class.getSimpleName();

    public static final int CMD_START = 10;
    public static final int CMD_STOP = 20;

    private Handler pollHandler;
    private Runnable pollRunnable;
    private Runnable journalPostRunnable;
    private Runnable serviceShutdownRunnable;
    static final int NETWORK_POLLING_MS = 5000;
    static final int NETWORK_JOURNAL_POST_TIMEOUT_MS = 30000;
    static final int SERVICE_SHUTDOWN_MS = 15000;
    private boolean requestToStop;
    private int pollingCount;
    private int logInAttempts;
    private BroadcastReceiver broadcastReceiver;
    private LAppServer server;

    public static void start(Context context) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", MainService.CMD_START);
        context.startService(serviceIntent);
    }

    public static void stop(Context context) {
        Intent serviceIntent = new Intent(context, MainService.class);
        serviceIntent.putExtra("cmd", MainService.CMD_STOP);
        context.startService(serviceIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        server = LAppServer.getInstance();

        DBScheduledTransaction.scanAlarm();

        pollHandler = new Handler() {
        };

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if ((!server.UiIsConnected()) && (pollingCount++ > 3)) {
                    LLog.d(TAG, "stop self: unable to connect, after " + pollingCount + " tries");
                    stopSelf();
                } else {
                    LLog.d(TAG, "heart beat polling");
                    server.UiPoll();
                    pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
                }
            }
        };

        journalPostRunnable = new Runnable() {
            @Override
            public void run() {
                LJournal.flush();
                pollHandler.postDelayed(journalPostRunnable, NETWORK_JOURNAL_POST_TIMEOUT_MS);
            }
        };

        serviceShutdownRunnable = new Runnable() {
            @Override
            public void run() {
                LLog.d(TAG, "shutdown timeout: shutting down service itself");
                requestToStop = false;
                stopSelf();
            }
        };

        broadcastReceiver = LBroadcastReceiver.getInstance().register(new int[]{
                LBroadcastReceiver.ACTION_NETWORK_CONNECTED,
                LBroadcastReceiver.ACTION_USER_CREATED,
                LBroadcastReceiver.ACTION_LOGIN,
                LBroadcastReceiver.ACTION_POLL_ACKED,
                LBroadcastReceiver.ACTION_POLL_IDLE,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SET_ACCOUNT_GID,
                LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_ACCOUNT_SHARE,
                LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_ACCOUNT_INFO,
                LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_SHARE_USER_PROFILE,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORD,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_CATEGORY,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_PAYER,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_TAG,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_PAYER_CATEGORY,

                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH,
                LBroadcastReceiver.ACTION_SHARE_ACCOUNT_WITH_USER,
                LBroadcastReceiver.ACTION_CONFIRMED_ACCOUNT_SHARE_WITH_UUID,
                LBroadcastReceiver.ACTION_JOURNAL_POSTED,
                LBroadcastReceiver.ACTION_JOURNAL_RECEIVED,
                LBroadcastReceiver.ACTION_SHARE_ACCOUNT_USER_CHANGE}, this);
    }

    @Override
    public void onDestroy() {
        pollHandler.removeCallbacks(serviceShutdownRunnable);
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.removeCallbacks(journalPostRunnable);
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
        server.disconnect();
        LLog.d(TAG, "service destroyed");

        pollRunnable = null;
        journalPostRunnable = null;
        serviceShutdownRunnable = null;
        pollHandler = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int cmd = intent.getIntExtra("cmd", 0);
            switch (cmd) {
                case CMD_START:
                    pollingCount = 0;
                    pollHandler.removeCallbacks(serviceShutdownRunnable);
                    requestToStop = false;
                    logInAttempts = 0;

                    LLog.d(TAG, "starting service, already connected: " + server.UiIsConnected());
                    if (server.UiIsConnected()) {
                        server.UiLogin();
                    } else {
                        server.connect();
                    }
                    break;

                case CMD_STOP:
                    LLog.d(TAG, "requested to stop service, connected: " + server.UiIsConnected());
                    if (server.UiIsConnected()) {
                        LLog.d(TAG, "post service shutdown runnable");
                        pollHandler.postDelayed(serviceShutdownRunnable, SERVICE_SHUTDOWN_MS);
                    } else {
                        stopSelf();
                    }
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onBroadcastReceiverReceive(int action, int ret, Intent intent) {
        //on Samsung phone, receiver may still get called when service is destroyed!!
        if (pollHandler == null) {
            LLog.w(TAG, "unexpected, receiver called on destroyed service!");
            return;
        }

        switch (action) {
            case LBroadcastReceiver.ACTION_NETWORK_CONNECTED:
                LLog.d(TAG, "network connected");
                server.UiInitScrambler();
                if (TextUtils.isEmpty(LPreferences.getUserName())) {
                    if (!TextUtils.isEmpty(LPreferences.getUserFullName()))
                        LLog.d(TAG, "user name empty but full name specified, request user name automatically");
                        server.UiRequestUserName();
                } else {
                    server.UiLogin();
                }
                break;

            case LBroadcastReceiver.ACTION_USER_CREATED:
                int userId = intent.getIntExtra("id", 0);
                String userName = intent.getStringExtra("name");
                LLog.d(TAG, "user created, id: " + userId + " name: " + userName);
                LPreferences.setUserId(userId);
                LPreferences.setUserName(userName);

                server.UiUpdateUserProfile();
                server.UiLogin();
                break;

            case LBroadcastReceiver.ACTION_LOGIN:
                if (ret == LProtocol.RSPS_OK) {
                    logInAttempts = 0;
                    server.UiUtcSync();
                    server.UiUpdateUserProfile();
                    LLog.d(TAG, "user logged in");
                    //journal posting and polling start only upon successful login
                    pollHandler.post(journalPostRunnable);
                    pollHandler.postDelayed(pollRunnable, 1000);
                } else {
                    LLog.e(TAG, "unable to login: " + LPreferences.getUserId() + " name: " + LPreferences.getUserName());
                    if (logInAttempts++ > 3) {
                        //the only reason that user is unable to login: user name no longer valid
                        // thus to wipe it off and let user to reset
                        //
                        LPreferences.setUserName("");
                    } else {
                        server.UiLogin();
                    }
                }
                break;

            case LBroadcastReceiver.ACTION_POLL_IDLE:
                if (LFragmentActivity.upRunning) {
                    server.UiUtcSync();
                } else if (!requestToStop){
                    requestToStop = true;
                    pollHandler.removeCallbacks(serviceShutdownRunnable);
                    pollHandler.postDelayed(serviceShutdownRunnable, SERVICE_SHUTDOWN_MS);
                }
                break;

            case LBroadcastReceiver.ACTION_POLL_ACKED:
                requestToStop = false;
                pollHandler.removeCallbacks(serviceShutdownRunnable);
                pollHandler.removeCallbacks(pollRunnable);
                pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
                LLog.d(TAG, "polling after being acked");
                server.UiPoll();
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH:
                int cacheId = intent.getIntExtra("cacheId", 0);
                server.UiPollAck(cacheId);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SET_ACCOUNT_GID:
                cacheId = intent.getIntExtra("cacheId", 0);
                userId = intent.getIntExtra("id", 0);
                int accountId = intent.getIntExtra("accountId", 0);
                int accountGid = intent.getIntExtra("accountGid", 0);
                String accountName = intent.getStringExtra("accountName");

                if (userId != LPreferences.getUserId()) {
                    LLog.e(TAG, "unexpected user id: " + userId + " myId: " + LPreferences.getUserId());
                } else {
                    LAccount account = DBAccount.getById(accountId);
                    if (account == null) {
                        LLog.w(TAG, "requested account no longer exist? id: " + accountId + " name: " + accountName);
                    } else {
                        boolean update = false;
                        if (!accountName.contentEquals(account.getName())) {
                            LLog.w(TAG, "account: " + accountId + " renamed before getting its GID: " + accountName + " -> " + account.getName());
                            account.setName(accountName);
                            update = true;
                        }
                        if (account.getGid() != 0 && accountGid != account.getGid()) {
                            LLog.w(TAG, "account GID already set: " + account.getGid() + " and mismatches new request: " + accountGid);
                        }
                        if (accountGid != account.getGid()) {
                            account.setGid(accountGid);
                            update = true;
                        }
                        if (update) DBAccount.update(account);
                    }
                }
                server.UiPollAck(cacheId);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_SHARE_USER_PROFILE:
                cacheId = intent.getIntExtra("cacheId", 0);
                userName = intent.getStringExtra("userName");
                userId = intent.getIntExtra("userId", 0);
                String userFullName = intent.getStringExtra("userFullName");
                LPreferences.setShareUserName(userId, userName);
                LPreferences.setShareUserFullName(userId, userFullName);
                server.UiPollAck(cacheId);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_ACCOUNT_SHARE:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                short numShareUsers = intent.getShortExtra("numShareUsers", (short)0);
                int[] shareUSers = intent.getIntArrayExtra("shareUsers");

                LLog.d(TAG, "requested to update account share for: " + accountGid);
                LAccount account = DBAccount.getByGid(accountGid);
                if (account == null) {
                    LLog.w(TAG, "requested account no longer exist? account gid: " + accountGid);
                } else {
                    LLog.d(TAG, "requested to update account share for: " + account.getName());
                    HashSet<Integer> newShareUsers = new HashSet<Integer>();
                    ArrayList<Integer> origIds = account.getShareIds();
                    ArrayList<Integer> origStates = account.getShareStates();

                    account.removeAllShareUsers();
                    int  myUserId = LPreferences.getUserId();
                    boolean isShared = false;
                    for (int user: shareUSers) if (user == myUserId) {isShared = true; break;}
                    LLog.d(TAG, "account: " + account.getName() + " shared? " + isShared);
                    if (isShared) {
                        for (int user : shareUSers) {
                            if (user == myUserId) continue;

                            boolean newShare = true;
                            for (int ii = 0; ii < origIds.size(); ii++) {
                                if (user == origIds.get(ii) && origStates.get(ii) == LAccount.ACCOUNT_SHARE_CONFIRMED_SYNCED) {
                                    newShare = false;
                                    break;
                                }
                            }
                            if (newShare) {
                                LLog.d(TAG, "new share from user: " + user);
                                newShareUsers.add(user);
                            }
                            account.addShareUser(user, LAccount.ACCOUNT_SHARE_CONFIRMED_SYNCED);
                        }
                    }
                    DBAccount.update(account);

                    //sync records to newly added share user
                    for (int user: newShareUsers) {
                        LJournal.pushAllAccountRecords(user, account);
                    }
                }
                server.UiPollAck(cacheId);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_ACCOUNT_INFO:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                accountName = intent.getStringExtra("accountName");
                String record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateAccountFromReceivedRecord(accountGid, accountName, record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORD:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateItemFromReceivedRecord(accountGid, record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_CATEGORY:
                cacheId = intent.getIntExtra("cacheId", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateCategoryFromReceivedRecord(record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_PAYER:
                cacheId = intent.getIntExtra("cacheId", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateVendorFromReceivedRecord(record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_TAG:
                cacheId = intent.getIntExtra("cacheId", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateTagFromReceivedRecord(record);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_PAYER_CATEGORY:
                cacheId = intent.getIntExtra("cacheId", 0);
                record = intent.getStringExtra("record");
                server.UiPollAck(cacheId);
                LJournal.updateVendorCategoryFromReceivedRecord(record);
                break;

            case LBroadcastReceiver.ACTION_JOURNAL_POSTED:
                requestToStop = false;
                pollHandler.removeCallbacks(serviceShutdownRunnable);
                pollHandler.removeCallbacks(journalPostRunnable);
                pollHandler.postDelayed(journalPostRunnable, NETWORK_JOURNAL_POST_TIMEOUT_MS);
                int journalId = intent.getIntExtra("journalId", 0);
                userId = intent.getIntExtra("userId", 0);
                switch (ret) {
                    case LProtocol.RSPS_OK: {
                        LJournal.deleteById(journalId);
                        LJournal.flush();
                        break;
                    }
                    case LProtocol.RSPS_USER_NOT_FOUND:
                    {
                        LLog.w(TAG, "journal post targeting user no longer available, id: " + userId);
                        LJournal.deleteById(journalId);
                        LJournal.flush();
                        break;
                    }
                    default:
                    {
                        // upon post error, do not flush immediately, instead let it timeout and retry
                        // this is to prevent flooding network layer when something goes wrong.
                        LLog.w(TAG, "unexpected journal post error, to: " + userId + " reture code: " + ret);
                        break;
                    }
                }
                break;

            case LBroadcastReceiver.ACTION_JOURNAL_RECEIVED:
                cacheId = intent.getIntExtra("cacheId", 0);
                server.UiPollAck(cacheId);

                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                //LLog.d(TAG, "received journal from: " + userId + "@" + userName + " ID: " + cacheId);
                LJournal.receive(intent.getStringExtra("record"));
                break;

            case LBroadcastReceiver.ACTION_UNKNOWN_MSG:
                LLog.w(TAG, "unknown message received");
            case LBroadcastReceiver.ACTION_SERVER_BROADCAST_MSG_RECEIVED:
                cacheId = intent.getIntExtra("cacheId", 0);
                server.UiPollAck(cacheId);
                break;
        }
    }
}
