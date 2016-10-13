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
    private BroadcastReceiver broadcastReceiver;

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
        DBScheduledTransaction.scanAlarm();

        pollHandler = new Handler() {
        };

        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if ((!LProtocol.ui.isConnected()) && (pollingCount++ > 3)) {
                    LLog.d(TAG, "stop self: unable to connect, after " + pollingCount + " tries");
                    stopSelf();
                } else {
                    LLog.d(TAG, "heart beat polling");
                    LProtocol.ui.poll();
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
                requestToStop = true;
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
                LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_SHARE_USER_PROFILE,
                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORD,

                LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH,
                LBroadcastReceiver.ACTION_SHARE_ACCOUNT_WITH_USER,
                LBroadcastReceiver.ACTION_CONFIRMED_ACCOUNT_SHARE_WITH_UUID,
                LBroadcastReceiver.ACTION_JOURNAL_POSTED,
                LBroadcastReceiver.ACTION_JOURNAL_RECEIVED,
                LBroadcastReceiver.ACTION_SHARE_ACCOUNT_USER_CHANGE}, this);
    }

    @Override
    public void onDestroy() {
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.removeCallbacks(journalPostRunnable);
        pollRunnable = null;
        journalPostRunnable = null;
        pollHandler = null;
        if (broadcastReceiver != null) {
            LBroadcastReceiver.getInstance().unregister(broadcastReceiver);
            broadcastReceiver = null;
        }
        LProtocol.ui.disconnect();
        LLog.d(TAG, "service destroyed");

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

                    LLog.d(TAG, "starting service, already connected: " + LProtocol.ui.isConnected());
                    if (LProtocol.ui.isConnected()) {
                        LProtocol.ui.login();
                    } else {
                        LProtocol.ui.connect();
                    }
                    break;

                case CMD_STOP:
                    LLog.d(TAG, "requested to stop service, connected: " + LProtocol.ui.isConnected());
                    if (LProtocol.ui.isConnected()) {
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
        switch (action) {
            case LBroadcastReceiver.ACTION_NETWORK_CONNECTED:
                LLog.d(TAG, "network connected");
                LProtocol.ui.initScrambler();
                if (TextUtils.isEmpty(LPreferences.getUserName())) {
                    if (!TextUtils.isEmpty(LPreferences.getUserFullName()))
                        LProtocol.ui.requestUserName();
                } else {
                    LProtocol.ui.login();
                }
                break;

            case LBroadcastReceiver.ACTION_USER_CREATED:
                int userId = intent.getIntExtra("id", 0);
                String userName = intent.getStringExtra("name");
                LPreferences.setUserId(userId);
                LPreferences.setUserName(userName);

                LProtocol.ui.updateUserProfile();
                LProtocol.ui.login();
                break;

            case LBroadcastReceiver.ACTION_LOGIN:
                if (ret == LProtocol.RSPS_OK) {
                    LProtocol.ui.utcSync();
                    LProtocol.ui.updateUserProfile();
                    LLog.d(TAG, "user logged in");
                    pollHandler.post(journalPostRunnable);
                    pollHandler.postDelayed(pollRunnable, 1000);
                } else {
                    LLog.w(TAG, "unable to login");
                    //the only reason that user is unable to login: user name no longer valid
                    // thus to wipe it off and let user to reset
                    LPreferences.setUserName("");
                }
                break;

            case LBroadcastReceiver.ACTION_POLL_IDLE:
                if (requestToStop || (!LFragmentActivity.upRunning)) {
                    LLog.d(TAG, "IDLE: stop self, requested: " + requestToStop + " active: " + LFragmentActivity.upRunning);
                    stopSelf();
                } else LProtocol.ui.utcSync();
                break;

            case LBroadcastReceiver.ACTION_POLL_ACKED:
                pollHandler.removeCallbacks(pollRunnable);
                pollHandler.postDelayed(pollRunnable, NETWORK_POLLING_MS);
                LLog.d(TAG, "polling after being acked");
                LProtocol.ui.poll();
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_ACCOUNT_WITH:
                int cacheId = intent.getIntExtra("cacheId", 0);
                LProtocol.ui.pollAck(cacheId);
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
                        if (!accountName.contentEquals(account.getName())) {
                            LLog.w(TAG, "account: " + accountId + " renamed before getting its GID: " + accountName + " -> " + account.getName());
                        }
                        if (account.getGid() != 0 && accountGid != account.getGid()) {
                            LLog.e(TAG, "account GID already set: " + account.getGid() + " and mismatches new request: " + accountGid);
                        } else if (accountGid != account.getGid()) {
                            account.setGid(accountGid);
                            DBAccount.update(account);
                        }
                    }
                }
                LProtocol.ui.pollAck(cacheId);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_SHARE_USER_PROFILE:
                cacheId = intent.getIntExtra("cacheId", 0);
                userName = intent.getStringExtra("userName");
                userId = intent.getIntExtra("userId", 0);
                String userFullName = intent.getStringExtra("userFullName");
                LPreferences.setShareUserName(userId, userName);
                LPreferences.setShareUserFullName(userId, userFullName);
                LProtocol.ui.pollAck(cacheId);
                break;

            case LBroadcastReceiver.ACTION_REQUESTED_TO_UPDATE_ACCOUNT_SHARE:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                short numShareUsers = intent.getShortExtra("numShareUsers", (short)0);
                int[] shareUSers = intent.getIntArrayExtra("shareUsers");

                LLog.d(TAG, "requested to update account share for: " + accountGid);
                LAccount account = DBAccount.getByGid(accountGid);
                if (account == null) {
                    LLog.w(TAG, "requested account no longer exist? GID: " + accountGid);
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
                LProtocol.ui.pollAck(cacheId);
                break;
/*
            case LBroadcastReceiver.ACTION_SHARE_ACCOUNT_WITH_USER:
                if (ret == LProtocol.RSPS_OK) {
                    int id = intent.getIntExtra("id", 0);
                    String name = LPreferences.getShareUserName(id);
                    accountName = intent.getStringExtra("accountName");
                    requireConfirmation = intent.getByteExtra("requireConfirmation", (byte) 0);

                    account = DBAccount.getByName(accountName);
                    account.addShareUser(id, requireConfirmation == 1 ? LAccount.ACCOUNT_SHARE_INVITED : LAccount.ACCOUNT_SHARE_CONFIRMED);
                    DBAccount.update(account);
                } else {
                    LLog.w(TAG, "unable to complete share request");
                    //displayErrorMsg(LShareAccountDialog.this.getContext().getString(R.string.warning_unable_to_complete_share_request));
                }

                break;

            case LBroadcastReceiver.ACTION_CONFIRMED_ACCOUNT_SHARE_WITH_UUID:
                cacheId = intent.getIntExtra("cacheId", 0);
                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                accountName = intent.getStringExtra("accountName");
                userFullName = intent.getStringExtra("userFullName");
                uuid = intent.getStringExtra("UUID");
                //TODO: notify user

                LPreferences.setShareUserName(userId, userName);
                LPreferences.setShareUserFullName(userId, userFullName);

                account = DBAccount.getByName(accountName);
                if (account == null) {
                    //TODO: the account name has been changed??
                    LLog.w(TAG, "warning: account renamed, account sharing ignored");
                } else {
                    if (uuid.compareTo(account.getRid()) > 0) account.setRid(uuid);

                    ArrayList<Integer> ids = account.getShareIds();
                    ArrayList<Integer> states = account.getShareStates();
                    for (int jj = 0; jj < ids.size(); jj++) {
                        if (states.get(jj) == LAccount.ACCOUNT_SHARE_CONFIRMED) {
                            LProtocol.ui.shareAccountUserChange(ids.get(jj), userId, true, account.getName(), account.getRid());
                        }
                    }

                    account.addShareUser(userId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                    DBAccount.update(account);
                }

                LProtocol.ui.pollAck(cacheId);

                // now push all existing records
                LJournal.pushAllAccountRecords(userId, account);
                break;
*/
            case LBroadcastReceiver.ACTION_REQUESTED_TO_SHARE_TRANSITION_RECORD:
                cacheId = intent.getIntExtra("cacheId", 0);
                accountGid = intent.getIntExtra("accountGid", 0);
                String record = intent.getStringExtra("record");
                LProtocol.ui.pollAck(cacheId);

                LJournal.updateItemFromReceivedRecord(accountGid, record);
                break;

            case LBroadcastReceiver.ACTION_JOURNAL_POSTED:
                pollHandler.removeCallbacks(journalPostRunnable);
                pollHandler.postDelayed(journalPostRunnable, NETWORK_JOURNAL_POST_TIMEOUT_MS);

                if (ret == LProtocol.RSPS_OK) {
                    int journalId = intent.getIntExtra("journalId", 0);
                    LJournal.deleteById(journalId);
                    LJournal.flush();
                } else {
                    // upon post error, do not flush immediately, instead let it timeout and retry
                    // this is to prevent flooding network layer when something goes wrong.
                    LLog.w(TAG, "journal post error");
                }
                break;

            case LBroadcastReceiver.ACTION_JOURNAL_RECEIVED:
                cacheId = intent.getIntExtra("cacheId", 0);
                LProtocol.ui.pollAck(cacheId);

                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                //LLog.d(TAG, "received journal from: " + userId + "@" + userName + " ID: " + cacheId);
                LJournal.receive(intent.getStringExtra("record"));
                break;
/*
            case LBroadcastReceiver.ACTION_SHARE_ACCOUNT_USER_CHANGE:
                // this is from our shared peer, informing status change for one of the existng shared user
                // or newly added user
                cacheId = intent.getIntExtra("cacheId", 0);
                LProtocol.ui.pollAck(cacheId);

                userId = intent.getIntExtra("id", 0);
                userName = intent.getStringExtra("userName");
                LLog.d(TAG, "received account share user change from: " + userId + "@" + userName);

                int changeUserId = intent.getIntExtra("changeUserId", 0);
                byte change = intent.getByteExtra("change", (byte) 0);
                accountName = intent.getStringExtra("accountName");
                uuid = intent.getStringExtra("UUID");

                account = DBAccount.getByName(accountName);
                if (account == null) {
                    LLog.w(TAG, "warning: account removed?");
                } else {
                    if (uuid.compareTo(account.getRid()) > 0) account.setRid(uuid);

                    if (change == 1) {
                        LLog.d(TAG, "account: " + accountName + " add share user: " + changeUserId);
                        //we receive this add request, *ONLY* because one of the shared peer accepted
                        //this user to share this account, so we add silently.
                        account.addShareUser(changeUserId, LAccount.ACCOUNT_SHARE_CONFIRMED);
                        DBAccount.update(account);

                        //this is to notify the counter part that we received the change request to add me
                        //basically this also says: hey, I confirm you to share account with me, and you've
                        //already got all my records (from one of my shared peer), now it is your turn to
                        //push me all your current records, so we're in sync.
                        LProtocol.ui.getShareUserById(changeUserId);
                        LProtocol.ui.shareAccountWithUser(changeUserId, accountName, account.getRid(), false);
                    } else {
                        LLog.d(TAG, "account: " + accountName + " remove share user: " + changeUserId);
                        if (changeUserId == LPreferences.getUserId()) {
                            //sorry, I am removed from the share group by one of the peer.
                            account.removeAllShareUsers();
                            LLog.d(TAG, "remove myself from share list");
                        } else {
                            //this is to remove somebody who was removed from group by someone else.
                            account.removeShareUser(changeUserId);
                        }
                        DBAccount.update(account);
                    }
                }
                break;
*/
            case LBroadcastReceiver.ACTION_UNKNOWN_MSG:
                LLog.w(TAG, "unknown message received");
            case LBroadcastReceiver.ACTION_SERVER_BROADCAST_MSG_RECEIVED:
                cacheId = intent.getIntExtra("cacheId", 0);
                LProtocol.ui.pollAck(cacheId);
                break;
        }
    }
}
